package io.github.xchat.features.items.batch

import androidx.activity.ComponentActivity
import dev.ujhhgtg.comptime.This
import io.github.xchat.features.api.core.WeConversationApi
import io.github.xchat.features.api.core.WeDatabaseApi
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.ui.content.ContactsSelector
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.showToast
import io.github.xchat.utils.android.showToastSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Feature(
    name = "批量标为已读",
    categories = ["批量操作"],
    description = "选择多个好友或群聊后, 将它们的对话一次性标记为已读"
)
object BatchMarkAsRead : ClickableFeature() {

    private val TAG = This.Class.simpleName

    override val noSwitchWidget = true

    override fun onClick(context: ComponentActivity) {
        val contacts = WeDatabaseApi.getFriends() + WeDatabaseApi.getGroups()

        showComposeDialog(context) {
            ContactsSelector(
                title = "选择要标为已读的对话",
                contacts = contacts,
                initialSelectedWxIds = emptySet(),
                onDismiss = onDismiss,
                onConfirm = { selectedWxIds ->
                    if (selectedWxIds.isEmpty()) {
                        showToast("请选择至少一个对话")
                        return@ContactsSelector
                    }

                    onDismiss()
                    markAsRead(selectedWxIds)
                }
            )
        }
    }

    private fun markAsRead(wxIds: Set<String>) {
        // These are local DB writes (no server CGI), so no rate-limit pacing is needed.
        CoroutineScope(Dispatchers.IO).launch {
            wxIds.forEach { wxId ->
                runCatching { WeConversationApi.markAsRead(wxId) }
                    .onFailure { WeLogger.e(TAG, "failed to mark $wxId as read", it) }
            }
            WeConversationApi.reloadConversations()
            showToastSuspend("已将 ${wxIds.size} 个对话标为已读")
        }
    }
}
