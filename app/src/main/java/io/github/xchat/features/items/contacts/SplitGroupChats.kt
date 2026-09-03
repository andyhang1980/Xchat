package io.github.xchat.features.items.contacts

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.tencent.mm.ui.chatting.ChattingUI
import dev.ujhhgtg.comptime.This
import io.github.xchat.features.api.core.WeDatabaseApi
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.ui.content.SingleContactSelector
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.WeLogger

@Feature(name = "分裂群组", categories = ["联系人与群组", "娱乐"], description = "让群聊一分为二; 在假群聊中发送的红包即为假红包")
object SplitGroupChats : ClickableFeature() {

    private val TAG = This.Class.simpleName

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            SingleContactSelector(
                "分裂群组",
                WeDatabaseApi.getGroups(),
                initialSelectedWxId = null,
                onDismiss = onDismiss,
            ) {
                onDismiss()
                jumpToSplitChatroom(context, it)
            }
        }
    }

    private fun jumpToSplitChatroom(context: Context, wxId: String) {
        runCatching {
            val rawId = wxId.substringBefore("@")
            val targetSplitId = "${rawId}@@chatroom"
            WeLogger.i(TAG, "launching ChattingUI for chatroom: $wxId")

            val intent = Intent(context, ChattingUI::class.java).apply {
                putExtra("Chat_User", targetSplitId)
                putExtra("Chat_Mode", 1)
            }

            context.startActivity(intent)
        }.onFailure { WeLogger.e(TAG, "exception occured", it) }
    }

    override val noSwitchWidget = true
}
