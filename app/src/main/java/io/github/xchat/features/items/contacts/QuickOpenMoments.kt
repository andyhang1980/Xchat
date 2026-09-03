package io.github.xchat.features.items.contacts

import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.api.ui.WeConversationContextMenuApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.features.items.chat.AggregateChats
import io.github.xchat.ui.utils.CameraIcon
import io.github.xchat.utils.strings.isGroupChatWxId

@Feature(name = "快捷打开朋友圈", categories = ["联系人与群组"], description = "在首页对话列表长按菜单添加菜单项, 可点击一键跳转到朋友圈")
object QuickOpenMoments : SwitchFeature(), WeConversationContextMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeConversationContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeConversationContextMenuApi.removeProvider(this)
    }

    override fun getMenuItems(): List<WeConversationContextMenuApi.MenuItem> {
        return listOf(
            WeConversationContextMenuApi.MenuItem(
                id = 777018,
                text = "朋友圈",
                drawable = CameraIcon,
                shouldShow = { context, _ ->
                    val talker = context.talker
                    talker.isNotEmpty() &&
                        !talker.isGroupChatWxId &&
                        !talker.startsWith("gh_") &&
                        !talker.endsWith("@app") &&
                        !talker.startsWith(AggregateChats.FOLDER_PREFIX)
                },
            ) { context ->
                WeApi.openMoments(context.activity, context.talker)
            }
        )
    }
}
