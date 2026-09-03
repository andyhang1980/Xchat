package io.github.xchat.features.items.chat

import android.view.View
import android.view.inputmethod.InputMethodManager
import dev.ujhhgtg.reflekt.reflekt
import io.github.xchat.features.api.core.WeMessageApi
import io.github.xchat.features.api.core.models.MessageType
import io.github.xchat.features.api.ui.WeChatMessageContextMenuApi
import io.github.xchat.features.api.ui.WeCurrentConversationApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.utils.EditIcon
import io.github.xchat.utils.android.getSystemService

@Feature(name = "一键撤回并重新编辑", categories = ["聊天"], description = "向消息长按菜单添加菜单项, 可快捷撤回消息并将文本内容加入输入框")
object QuickRevokeAndEdit : SwitchFeature(), WeChatMessageContextMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            WeChatMessageContextMenuApi.MenuItem(
                777016, "编辑", EditIcon,
                shouldShow = {
                    @Suppress("DEPRECATION")
                    it.type == MessageType.TEXT
                }
            ) { view, _, msgInfo ->
                val chatFooter = WeCurrentConversationApi.chatFooter ?: return@MenuItem
                WeMessageApi.revokeMsg(msgInfo)
                chatFooter.lastText = msgInfo.actualContent

                chatFooter.setMode(1)
                val toSendEt = chatFooter.reflekt().invokeMethod("getToSendEt")!!

                val etView = toSendEt.reflekt().firstMethod {
                    returnType = View::class
                }.invoke()!! as View

                etView.requestFocus()
                val context = view.context
                val im = context.getSystemService<InputMethodManager>()
                etView.post {
                    im.showSoftInput(etView, 0)
                }
            }
        )
    }
}
