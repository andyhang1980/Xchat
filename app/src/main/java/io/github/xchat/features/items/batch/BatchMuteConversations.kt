package io.github.xchat.features.items.batch

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import dev.ujhhgtg.comptime.This
import io.github.xchat.features.api.core.WeConversationApi
import io.github.xchat.features.api.core.WeDatabaseApi
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.ContactsSelector
import io.github.xchat.ui.content.DefaultColumn
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.showToast
import io.github.xchat.utils.android.showToastSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Feature(
    name = "批量免打扰",
    categories = ["批量操作"],
    description = "选择多个好友或群聊后, 批量开启或关闭消息免打扰 (仅本地生效, 不同步至服务器)"
)
object BatchMuteConversations : ClickableFeature() {

    private val TAG = This.Class.simpleName

    override val noSwitchWidget = true

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            AlertDialogContent(
                title = { Text("批量免打扰") },
                text = {
                    DefaultColumn {
                        ListItem(
                            headlineContent = { Text("开启免打扰") },
                            supportingContent = { Text("选择要静音的对话") },
                            modifier = Modifier.clickable {
                                onDismiss()
                                pickAndApply(context, mute = true)
                            }
                        )
                        ListItem(
                            headlineContent = { Text("关闭免打扰") },
                            supportingContent = { Text("选择要取消静音的对话") },
                            modifier = Modifier.clickable {
                                onDismiss()
                                pickAndApply(context, mute = false)
                            }
                        )
                    }
                },
                dismissButton = { TextButton(onDismiss) { Text("取消") } }
            )
        }
    }

    private fun pickAndApply(context: Context, mute: Boolean) {
        val contacts = WeDatabaseApi.getFriends() + WeDatabaseApi.getGroups()

        showComposeDialog(context) {
            ContactsSelector(
                title = if (mute) "选择要静音的对话" else "选择要取消静音的对话",
                contacts = contacts,
                initialSelectedWxIds = emptySet(),
                onDismiss = onDismiss,
                onConfirm = { selectedWxIds ->
                    if (selectedWxIds.isEmpty()) {
                        showToast("请选择至少一个对话")
                        return@ContactsSelector
                    }

                    onDismiss()
                    apply(selectedWxIds, mute)
                }
            )
        }
    }

    private fun apply(wxIds: Set<String>, mute: Boolean) {
        // Local DB writes only, so no rate-limit pacing is needed.
        CoroutineScope(Dispatchers.IO).launch {
            wxIds.forEach { wxId ->
                runCatching { WeConversationApi.setIfNotifyNewMessages(wxId, !mute) }
                    .onFailure { WeLogger.e(TAG, "failed to set mute=$mute for $wxId", it) }
            }
            WeConversationApi.reloadConversations()
            showToastSuspend(
                if (mute) "已对 ${wxIds.size} 个对话开启免打扰"
                else "已对 ${wxIds.size} 个对话关闭免打扰"
            )
        }
    }
}
