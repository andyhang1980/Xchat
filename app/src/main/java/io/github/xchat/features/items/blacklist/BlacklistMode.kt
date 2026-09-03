package io.github.xchat.features.items.blacklist

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ujhhgtg.comptime.This
import io.github.xchat.features.api.core.WeContactApi
import io.github.xchat.features.api.core.WeDatabaseApi
import io.github.xchat.features.api.core.models.WeContact
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.preferences.WePrefs
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.ContactsSelector
import io.github.xchat.ui.content.DefaultColumn
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.copyToClipboard
import io.github.xchat.utils.android.showToast
import io.github.xchat.utils.android.showToastSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 黑名单模式: 在黑名单里的微信 ID (wxId), 一键删除并拉黑。
 *
 * 工作流:
 *  1. 在此页查看所有好友的微信 ID (wxId / 微信号), 可复制。
 *  2. 将想拉黑的 ID 加入黑名单 (逐个 / 从通讯录批量勾选 / 手动粘贴)。
 *  3. 点击「执行删除并拉黑」, 对黑名单中的每个 ID 执行拉黑并删除 (自动间隔规避风控)。
 *
 * 注: 黑名单可同时包含联系人 ID 与群聊 (xxx@chatroom) ID; 群聊的删除受不同微信版本服务端支持情况影响。
 */
@Feature(
    name = "黑名单模式",
    categories = ["联系人"],
    description = "在黑名单里的微信 ID 一键删除并拉黑\n1. 查看/复制全部好友微信 ID\n2. 将目标 ID 加入黑名单 (单个 / 批量 / 粘贴)\n3. 一键删除并拉黑黑名单中的全部账号 (自动间隔规避风控)"
)
object BlacklistMode : ClickableFeature() {

    private val TAG = This.Class.simpleName

    override val noSwitchWidget = true

    /** 空格分隔的黑名单, 避免单次网络请求过大. */
    private const val DELETE_INTERVAL_MS = 1500L

    /** 持久化黑名单 (wxId 集合). */
    private var blacklist by WePrefs.prefOption("blacklist_mode_ids", emptySet<String>())

    /** 由 (wxId -> 展示名) 决定的临时解析映射, 刷新自数据库. */
    private fun resolveNames(): Map<String, String> = buildMap {
        runCatching { WeDatabaseApi.getFriends() }.getOrNull().orEmpty().forEach { put(it.wxId, it.displayName) }
        runCatching { WeDatabaseApi.getGroups() }.getOrNull().orEmpty().forEach { put(it.wxId, it.displayName) }
    }

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            BlacklistPanel(
                blacklist = blacklist,
                onAdd = { id ->
                    val v = id.trim()
                    if (v.isNotEmpty()) {
                        blacklist = blacklist + v
                        showToast("已加入黑名单: $v")
                    }
                },
                onRemove = { id ->
                    blacklist = blacklist - id
                },
                onCopyAll = {
                    copyToClipboard(blacklist.joinToString("\n"))
                    showToast("已复制 ${blacklist.size} 个 ID")
                },
                onBatchSelect = {
                    onDismiss()
                    selectFromContacts(context)
                },
                onExecute = {
                    onDismiss()
                    executeBlacklist(context)
                },
                onDismiss = onDismiss
            )
        }
    }

    /** 从通讯录批量勾选好友, 添加到黑名单. */
    private fun selectFromContacts(context: ComponentActivity) {
        val friends = runCatching { WeDatabaseApi.getFriends() }.getOrNull().orEmpty()
        if (friends.isEmpty()) {
            showToast("未获取到好友列表")
            return
        }

        showComposeDialog(context) {
            ContactsSelector(
                title = "选择要加入黑名单的好友",
                contacts = friends,
                initialSelectedWxIds = blacklist,
                onDismiss = onDismiss,
                onConfirm = { selectedWxIds ->
                    if (selectedWxIds.isEmpty()) {
                        showToast("未选择任何好友")
                        return@ContactsSelector
                    }
                    blacklist = blacklist + selectedWxIds
                    onDismiss()
                    showToast("已添加 ${selectedWxIds.size} 个 ID 到黑名单")
                }
            )
        }
    }

    /** 对黑名单中的每个 ID 执行拉黑并删除. */
    private fun executeBlacklist(context: Context) {
        val ids = blacklist.toList()
        if (ids.isEmpty()) {
            showToast("黑名单为空")
            return
        }

        showComposeDialog(context) {
            AlertDialogContent(
                title = { Text("确认执行") },
                text = { Text("将对黑名单中的 ${ids.size} 个账号执行「拉黑并删除」。此操作不可逆, 确定继续吗?") },
                dismissButton = { TextButton(onDismiss) { Text("取消") } },
                confirmButton = {
                    Button(onClick = {
                        onDismiss()
                        CoroutineScope(Dispatchers.IO).launch {
                            execute(ids)
                        }
                    }) { Text("确定") }
                }
            )
        }
    }

    private suspend fun execute(ids: List<String>) {
        showToastSuspend("正在删除并拉黑 ${ids.size} 个账号...")
        val succeeded = mutableListOf<String>()
        var failed = 0
        ids.forEachIndexed { index, wxId ->
            val ok = runCatching { WeContactApi.deleteContact(wxId, WeContactApi.DeleteMode.BLOCK_AND_DELETE) }.getOrNull() ?: false
            if (ok) succeeded.add(wxId) else failed++
            WeLogger.i(TAG, "blacklist delete $wxId -> $ok (${succeeded.size + failed}/${ids.size})")
            if (index < ids.size - 1) delay(DELETE_INTERVAL_MS)
        }

        // 成功删除的移出黑名单, 失败的保留以便重试
        blacklist = blacklist - succeeded.toSet()
    }

    @Composable
    private fun BlacklistPanel(
        blacklist: Set<String>,
        onAdd: (String) -> Unit,
        onRemove: (String) -> Unit,
        onCopyAll: () -> Unit,
        onBatchSelect: () -> Unit,
        onExecute: () -> Unit,
        onDismiss: () -> Unit,
    ) {
        val nameMap = remember { resolveNames() }
        var manualId by remember { mutableStateOf("") }
        var showFriendId by remember { mutableStateOf(true) }

        AlertDialogContent(
            title = { Text("黑名单模式") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    // ---- 操作按钮 ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onBatchSelect, modifier = Modifier.weight(1f)) { Text("从通讯录勾选") }
                        Button(onClick = onCopyAll, modifier = Modifier.weight(1f)) { Text("复制全部ID") }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ---- 手动输入 ----
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = manualId,
                            onValueChange = { manualId = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("粘贴微信 ID 或群 ID") },
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            onAdd(manualId)
                            manualId = ""
                        }) { Text("添加") }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ---- 当前黑名单 ----
                    Text("当前黑名单 (${blacklist.size})", style = MaterialTheme.typography.titleSmall)
                    if (blacklist.isEmpty()) {
                        Text(
                            "暂无黑名单。可从上方通讯录勾选、粘贴 ID, 或在下方「好友ID」列表中点「+」加入。",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        blacklist.sorted().forEach { id ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        nameMap[id] ?: "未知",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(id, style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { onRemove(id) }) { Text("移除") }
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))

                    // ---- 好友 ID 切换开关 ----
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("显示好友ID", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(4.dp))
                        Switch(checked = showFriendId, onCheckedChange = { showFriendId = it }) {}
                    }

                    // ---- 好友 ID 列表 (便于复制并加入黑名单) ----
                    if (showFriendId) {
                        Text("好友微信ID", style = MaterialTheme.typography.titleSmall)
                        val friends = remember {
                            runCatching { WeDatabaseApi.getFriends() }.getOrNull().orEmpty()
                        }
                        if (friends.isEmpty()) {
                            Text("未获取到好友列表", style = MaterialTheme.typography.bodySmall)
                        } else {
                            friends.forEach { f ->
                                FriendIdRow(f, inBlacklist = f.wxId in blacklist, onAdd = { onAdd(f.wxId) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onExecute) { Text("执行删除并拉黑") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        )
    }

    @Composable
    private fun FriendIdRow(f: WeContact, inBlacklist: Boolean, onAdd: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    f.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append("ID: ").append(f.wxId)
                        if (f.customWxId.isNotBlank()) append("  微信号: ").append(f.customWxId)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = {
                copyToClipboard(f.wxId)
                showToast("已复制 ${f.wxId}")
            }) { Text("复制") }
            if (inBlacklist) {
                Text("已加入", style = MaterialTheme.typography.bodySmall)
            } else {
                TextButton(onClick = onAdd) { Text("+") }
            }
        }
    }
}
