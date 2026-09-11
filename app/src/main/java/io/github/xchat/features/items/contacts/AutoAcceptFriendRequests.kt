package io.github.xchat.features.items.contacts

import io.github.xchat.features.core.Feature

import android.annotation.SuppressLint
import android.content.ContentValues
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexConstructor
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.api.core.WeDatabaseApi
import io.github.xchat.features.api.core.WeDatabaseListenerApi
import io.github.xchat.features.api.core.WeMessageApi
import io.github.xchat.features.api.net.WeNetSceneApi
import io.github.xchat.features.api.core.models.MessageInfo
import io.github.xchat.features.api.core.models.MessageType
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.luckypray.dexkit.DexKitBridge
import kotlin.random.Random

@SuppressLint("SetTextI18n")
@Feature(
    name = "自动同意好友申请",
    categories = ["联系人与群组"],
    description = "自动同意好友申请，支持延迟设置、自动发送欢迎语、黑名单过滤"
)
object AutoAcceptFriendRequests : ClickableFeature(), IResolveDex,
    WeDatabaseListenerApi.IInsertListener {

    private const val TAG = "AutoAcceptFriendReq"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private var masterEnabled by prefOption("aafr_master_enabled", false)
    private var delayMode by prefOption("aafr_delay_mode", 0)
    private var fixedDelayMs by prefOption("aafr_fixed_delay_ms", 1000)
    private var randomDelayMinMs by prefOption("aafr_random_delay_min_ms", 1000)
    private var randomDelayMaxMs by prefOption("aafr_random_delay_max_ms", 5000)
    private var welcomeText by prefOption("aafr_welcome_text", "你好，我已通过你的好友申请")
    private var sendWelcome by prefOption("aafr_send_welcome", true)
    private var blacklistJson by prefOption("aafr_blacklist", "[]")

    private val processedRequests = mutableSetOf<String>()

    private fun getBlacklist(): Set<String> {
        return runCatching {
            json.decodeFromString(SetSerializer(String.serializer()), blacklistJson)
        }.getOrDefault(emptySet())
    }

    enum class DelayMode(val value: Int, val description: String) {
        FIXED(0, "固定延迟"),
        RANDOM(1, "随机延迟")
    }

    private val methodVerifyAccept by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("This NetSceneVerifyUser init MUST use opcode == MM_VERIFYUSER_VERIFYOK")
        }
    }

    private val methodVerifyOkClick by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.VerifyUserUtil", "verify ok clicked")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodVerifyAccept.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("This NetSceneVerifyUser init MUST use opcode == MM_VERIFYUSER_VERIFYOK")
            }
        }
        methodVerifyOkClick.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.VerifyUserUtil", "verify ok clicked")
            }
        }
    }

    override fun onEnable() {
        WeDatabaseListenerApi.addListener(this)
        val verifyUnavailable = methodVerifyAccept.isPlaceholder
        val verifyOkUnavailable = methodVerifyOkClick.isPlaceholder
        if (verifyUnavailable && verifyOkUnavailable) {
            WeLogger.w(TAG, "当前微信版本暂不支持自动同意好友申请")
            return
        }
        runCatching {
            methodVerifyAccept.hookAfter {
                if (args.size < 3) return@hookAfter
                if (!masterEnabled) return@hookAfter
                val opcode = args[0] as? Int ?: return@hookAfter
                if (opcode != 1 && opcode != 2) return@hookAfter
                val verifyContent = (args[1] as? String)?.takeIf { it.isNotBlank() } ?: return@hookAfter
                if (sendWelcome && welcomeText.isNotBlank()) {
                    val targetWxId = findNewFriendWxId(verifyContent)
                    if (targetWxId.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(1500)
                            runCatching {
                                WeMessageApi.sendText(targetWxId, welcomeText)
                            }.onFailure { e -> WeLogger.e(TAG, "failed to send welcome", e) }
                        }
                    }
                }
            }
        }.onFailure { e -> WeLogger.e(TAG, "failed to hook verify accept", e) }
    }

    override fun onDisable() {
        WeDatabaseListenerApi.removeListener(this)
        processedRequests.clear()
    }

    override fun onInsert(table: String, values: ContentValues) {
        if (table != "message") return
        if (!masterEnabled) return
        val msgInfo = runCatching { MessageInfo.fromContentValues(values) }.getOrNull() ?: return
        if (msgInfo.isSelfSender) return
        if (msgInfo.typeCode != MessageType.FRIEND_VERIFY.code) return
        val content = msgInfo.content ?: return
        if (content.isEmpty()) return
        val encryptUsername = extractXmlValue(content, "encryptusername")
        val ticket = extractXmlValue(content, "ticket")
        if (encryptUsername.isNullOrEmpty() || ticket.isNullOrEmpty()) return
        val requestKey = "$encryptUsername:$ticket"
        if (requestKey in processedRequests) return
        processedRequests.add(requestKey)
        if (processedRequests.size > 200) processedRequests.clear()
        val blacklist = getBlacklist()
        if (encryptUsername in blacklist) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val delayMs = when (delayMode) {
                    1 -> Random.nextLong(randomDelayMinMs.toLong(), (randomDelayMaxMs + 1).toLong())
                    else -> fixedDelayMs.toLong()
                }
                if (delayMs > 0) delay(delayMs)
                acceptFriendRequest(encryptUsername, ticket)
            }.onFailure { e -> WeLogger.e(TAG, "failed", e) }
        }
    }

    private fun acceptFriendRequest(encryptUsername: String, ticket: String) {
        if (!methodVerifyAccept.isPlaceholder) {
            methodVerifyAccept.method.invoke(null, 2, "v2_$encryptUsername@$ticket@", "", "")
        } else {
            WeLogger.w(TAG, "methodVerifyAccept not available, using fallback")
        }
    }

    private fun findNewFriendWxId(encryptUsername: String): String {
        return runCatching {
            WeDatabaseApi.rawQuery("SELECT username FROM rcontact WHERE encryptUsername=?", arrayOf(encryptUsername))
                .use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) ?: "" else "" }
        }.getOrDefault("")
    }

    private fun extractWxIdFromVerifyContent(verifyContent: String): String {
        return runCatching {
            val parts = verifyContent.split("@")
            if (parts.size >= 2) { findNewFriendWxId(parts[0].removePrefix("v2_")) } else ""
        }.getOrDefault("")
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val regex = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        return regex.find(xml)?.groupValues?.getOrNull(1)?.trim()
    }

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var localMasterEnabled by remember { mutableStateOf(masterEnabled) }
            var localDelayMode by remember { mutableStateOf(delayMode) }
            var localFixedDelayMs by remember { mutableStateOf(fixedDelayMs) }
            var localRandomMinMs by remember { mutableStateOf(randomDelayMinMs) }
            var localRandomMaxMs by remember { mutableStateOf(randomDelayMaxMs) }
            var localWelcomeText by remember { mutableStateOf(welcomeText) }
            var localSendWelcome by remember { mutableStateOf(sendWelcome) }
            var localBlacklist by remember { mutableStateOf(getBlacklist().joinToString("\n")) }

            AlertDialogContent(
                title = { Text("自动同意好友申请") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ListItem(
                            modifier = Modifier.clickable { localMasterEnabled = !localMasterEnabled },
                            trailingContent = { Switch(checked = localMasterEnabled, onCheckedChange = null) },
                            headlineContent = { Text("启用自动同意", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("开启后自动同意收到的所有好友申请") }
                        )
                        if (localMasterEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("延迟设置", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            DelayMode.values().forEach { mode ->
                                ListItem(
                                    modifier = Modifier.clickable { localDelayMode = mode.value },
                                    trailingContent = { Text(if (localDelayMode == mode.value) "✓" else "") },
                                    headlineContent = { Text(mode.description) }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            ListItem(
                                modifier = Modifier.clickable { localSendWelcome = !localSendWelcome },
                                trailingContent = { Switch(checked = localSendWelcome, onCheckedChange = null) },
                                headlineContent = { Text("自动发送欢迎语") },
                                supportingContent = { Text("通过好友申请后自动发送一条消息") }
                            )
                            if (localSendWelcome) {
                                OutlinedTextField(
                                    value = localWelcomeText, onValueChange = { localWelcomeText = it },
                                    label = { Text("欢迎语内容") }, minLines = 2,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("黑名单管理", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = localBlacklist, onValueChange = { localBlacklist = it },
                                label = { Text("黑名单 wxId") }, minLines = 3, maxLines = 8,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            )
                        }
                    }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
                confirmButton = {
                    Button(onClick = {
                        masterEnabled = localMasterEnabled
                        delayMode = localDelayMode
                        fixedDelayMs = localFixedDelayMs
                        randomDelayMinMs = localRandomMinMs
                        randomDelayMaxMs = localRandomMaxMs
                        welcomeText = localWelcomeText
                        sendWelcome = localSendWelcome
                        blacklistJson = json.encodeToString(getBlacklist().toSet())
                        showToast("设置已保存")
                        onDismiss()
                    }) { Text("保存") }
                }
            )
        }
    }
}
