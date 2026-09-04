package io.github.xchat.features.items.chat

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Alternate_email
import com.composables.icons.materialsymbols.outlined.Send_time_extension
import com.composables.icons.materialsymbols.outlined.Text_to_speech
import com.composables.icons.materialsymbols.outlined.Voice_chat
import com.tencent.mm.pluginsdk.ui.chat.ChatFooter
import dev.ujhhgtg.comptime.nameOf
import dev.ujhhgtg.reflekt.reflekt
import io.github.xchat.activity.TransparentActivity
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.api.core.WeDatabaseApi
import io.github.xchat.features.api.core.WeMessageApi
import io.github.xchat.features.api.net.WePacketHelper
import io.github.xchat.features.api.ui.WeCurrentConversationApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.preferences.WePrefs
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.findViewByChildIndexes
import io.github.xchat.ui.utils.findViewWhich
import io.github.xchat.ui.utils.findViewsWhich
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.AudioUtils
import io.github.xchat.utils.EdgeTtsClient
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.showToast
import io.github.xchat.utils.android.showToastSuspend
import io.github.xchat.utils.coerceToInt
import io.github.xchat.utils.fileExtension
import io.github.xchat.utils.fs.KnownPaths
import io.github.xchat.utils.strings.isGroupChatWxId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.outputStream
import android.widget.Button as AndroidButton

@Feature(
    name = "聊天输入栏增强",
    categories = ["聊天"],
    description = "为聊天输入栏添加更多功能\n1. 在聊天界面长按「发送」或「加号菜单」按钮打开菜单\n菜单功能: 「发送语音文件」「文本转语音发送 (长按选音色)」「发送卡片消息」「@所有人」\n2. 长按「语音」按钮发送自定义语音文件 (SILK 或 MP3)"
)
object ChatInputBarEnhancements : SwitchFeature(), IResolveDex {

    // 文本转语音可选音色 (Edge TTS voice name -> 展示名称)。
    private val TTS_VOICES = listOf(
        "zh-CN-XiaoxiaoNeural" to "晓晓 (女, 温柔)",
        "zh-CN-XiaoyiNeural" to "晓伊 (女, 活泼)",
        "zh-CN-YunxiNeural" to "云希 (男, 阳光)",
        "zh-CN-YunyangNeural" to "云扬 (男, 播报)",
        "zh-CN-YunjianNeural" to "云健 (男, 浑厚)",
        "zh-CN-YunxiaNeural" to "云夏 (男, 少年)",
        "zh-CN-liaoning-XiaobeiNeural" to "晓北 (女, 东北话)",
        "zh-CN-shaanxi-XiaoniNeural" to "晓妮 (女, 陕西话)",
        "zh-HK-HiuMaanNeural" to "曉曼 (女, 粤语)",
        "zh-HK-WanLungNeural" to "雲龍 (男, 粤语)",
        "zh-TW-HsiaoChenNeural" to "曉臻 (女, 台湾)",
        "zh-TW-YunJheNeural" to "雲哲 (男, 台湾)",
        "en-US-AriaNeural" to "Aria (女, 英语)",
        "en-US-GuyNeural" to "Guy (男, 英语)",
        "ja-JP-NanamiNeural" to "七海 (女, 日语)",
    )

    private const val DEFAULT_TTS_VOICE = "zh-CN-XiaoxiaoNeural"

    private var ttsVoice by WePrefs.prefOption("chat_tts_voice", DEFAULT_TTS_VOICE)

    val methodSendMessage by dexMethod {
        searchPackages("com.tencent.mm.pluginsdk.ui.chat")
        matcher {
            usingEqStrings("MicroMsg.ChatFooter", "send msg onClick")
        }
    }

    override fun onEnable() {
        ChatFooter::class.reflekt()
            .firstConstructor {
                parameters(Context::class, AttributeSet::class, Int::class)
            }.hookAfter {
                WeLogger.d(nameOf(ChatInputBarEnhancements), "ChatFooter hookAfter fired")
                val chatFooter = thisObject as ChatFooter
                val searchedView = chatFooter.findViewByChildIndexes<View>(0)!!
                val imgButtons = searchedView.findViewsWhich<ImageButton> { view ->
                    view.javaClass.simpleName == "WeImageButton"
                }
                val voiceButton = imgButtons.first()
                val menuButton = imgButtons.last()
                val sendButton = searchedView.findViewWhich<AndroidButton> { view ->
                    view.javaClass.name == "android.widget.Button" && run {
                        val text = (view as AndroidButton).text?.toString()?.trim() ?: ""
                        text == "发送" || text.equals("send", ignoreCase = true)
                    }
                }
                WeLogger.d(nameOf(ChatInputBarEnhancements), "sendButton found: ${sendButton != null}")

                voiceButton.setOnLongClickListener { view ->
                    WeLogger.d(nameOf(ChatInputBarEnhancements), "voiceButton long click")
                    val content = chatFooter.lastText
                    if (content.isEmpty()) {
                        showToast("输入内容为空!")
                        return@setOnLongClickListener true
                    }
                    synthesizeAndSendVoice(
                        WeCurrentConversationApi.value, content, ttsVoice
                    ) { chatFooter.lastText = "" }
                    return@setOnLongClickListener true
                }

                menuButton.setOnLongClickListener { view ->
                    val content = chatFooter.lastText
                    if (content.isEmpty()) {
                        showToast("输入内容为空!")
                        return@setOnLongClickListener true
                    }
                    synthesizeAndSendVoice(
                        WeCurrentConversationApi.value, content, ttsVoice
                    ) { chatFooter.lastText = "" }
                    return@setOnLongClickListener true
                }

                sendButton?.setOnLongClickListener { view ->
                    WeLogger.d(nameOf(ChatInputBarEnhancements), "sendButton long click, text='${chatFooter.lastText}'")
                    val content = chatFooter.lastText
                    if (content.isEmpty()) {
                        showToast("输入内容为空!")
                        return@setOnLongClickListener true
                    }
                    synthesizeAndSendVoice(
                        WeCurrentConversationApi.value, content, ttsVoice
                    ) { chatFooter.lastText = "" }
                    return@setOnLongClickListener true
                }
            }
    }

    /** 弹出音色单选列表, 选中即用 [WePrefs] 持久化到 [ttsVoice]。 */
    private fun showVoicePicker(context: Context) {
        showComposeDialog(context) {
            var selected by remember { mutableStateOf(ttsVoice) }
            AlertDialogContent(
                title = { Text("选择音色") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        TTS_VOICES.forEach { (voice, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selected = voice }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selected == voice,
                                    onClick = { selected = voice }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                dismissButton = { TextButton(onDismiss) { Text("取消") } },
                confirmButton = {
                    Button(onClick = {
                        ttsVoice = selected
                        showToast("音色已保存")
                        onDismiss()
                    }) { Text("确定") }
                }
            )
        }
    }
}

private fun selectAndSendVoice(context: Context, currentConv: String) {
    TransparentActivity.launch(context) {
        val importLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) {
                finish()
                return@registerForActivityResult
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val tempPath = KnownPaths.moduleCache / "voice_tmp.${uri.fileExtension.ifEmpty { ".mp3" }}"
                contentResolver.openInputStream(uri)!!.use { input ->
                    tempPath.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val mimeType = contentResolver.getType(uri) ?: return@launch
                val isSilk = mimeType == "audio/amr"
                showToastSuspend("语音文件准备完成")
                val durationMs = AudioUtils.getDurationMs(tempPath.absolutePathString())

                withContext(Dispatchers.Main) {
                    finish()
                    showComposeDialog(context) {
                        var durationInput by remember { mutableStateOf(durationMs.toString()) }
                        AlertDialogContent(
                            title = { Text("发送语音文件") },
                            text = {
                                TextField(
                                    value = durationInput,
                                    onValueChange = { durationInput = it.filter { c -> c.isDigit() } },
                                    label = { Text("语音时长 (毫秒)") })
                            },
                            dismissButton = { TextButton(onDismiss) { Text("取消") } },
                            confirmButton = {
                                Button(onClick = {
                                    val durationMs = durationInput.toLongOrNull()
                                    if (durationMs == null) {
                                        showToast("时长格式不正确!")
                                        return@Button
                                    }

                                    var success = false
                                    if (isSilk) {
                                        showToast("正在发送 SILK...")
                                        success = WeMessageApi.sendVoice(
                                            currentConv,
                                            tempPath.absolutePathString(),
                                            durationMs.coerceToInt()
                                        )
                                    } else {
                                        showToast("正在将 MP3 转换为 SILK...")
                                        val tempSilkPath = KnownPaths.moduleCache / "voice_conv_tmp"
                                        val convSuccess = AudioUtils.mp3ToSilk(
                                            tempPath.absolutePathString(),
                                            tempSilkPath.absolutePathString()
                                        )
                                        if (convSuccess) {
                                            showToast("转换成功! 正在发送...")
                                            success = WeMessageApi.sendVoice(
                                                currentConv,
                                                tempSilkPath.absolutePathString(),
                                                durationMs.coerceToInt()
                                            )
                                        } else {
                                            showToast("转换失败! 查看日志以了解错误详情")
                                        }
                                        tempSilkPath.deleteIfExists()
                                    }
                                    showToast("语音发送${if (success) "成功" else "失败!"}")
                                    tempPath.deleteIfExists()
                                    onDismiss()
                                }) { Text("确定") }
                            })
                    }
                }
            }
        }
        // android couldn't distinguish AMR-extension SILK files, so we just use amr here
        importLauncher.launch(arrayOf("audio/amr", "audio/mpeg"))
    }
}

/**
 * 用 [EdgeTtsClient] 把文本合成为 MP3, 转成 SILK 后作为语音消息发送。
 * 全程在 IO 线程执行, 完成后回到主线程执行 [onSent] (例如清空输入框)。
 */
private fun synthesizeAndSendVoice(
    currentConv: String,
    text: String,
    voice: String,
    onSent: () -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        showToastSuspend("正在合成语音...")
        val mp3Path = KnownPaths.moduleCache / "tts_tmp.mp3"
        val silkPath = KnownPaths.moduleCache / "tts_conv_tmp"
        try {
            EdgeTtsClient.synthesizeToMp3(text, mp3Path, voice = voice).onFailure {
                WeLogger.d(nameOf(ChatInputBarEnhancements), "failed to synthesize voice", it)
                showToastSuspend("语音合成失败! 错因: ${it.message}")
                return@launch
            }

            val durationMs = AudioUtils.getDurationMs(mp3Path.absolutePathString())
            showToastSuspend("合成成功, 正在转换并发送...")

            val convSuccess = AudioUtils.mp3ToSilk(
                mp3Path.absolutePathString(),
                silkPath.absolutePathString(),
            )
            if (!convSuccess) {
                showToastSuspend("MP3 转 SILK 失败! 查看日志以了解错误详情")
                return@launch
            }

            val success = WeMessageApi.sendVoice(
                currentConv,
                silkPath.absolutePathString(),
                durationMs.coerceToInt(),
            )
            showToastSuspend("语音发送${if (success) "成功" else "失败!"}")
            if (success) {
                withContext(Dispatchers.Main) { onSent() }
            }
        } finally {
            mp3Path.deleteIfExists()
            silkPath.deleteIfExists()
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
