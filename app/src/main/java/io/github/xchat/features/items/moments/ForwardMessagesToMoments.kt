package io.github.xchat.features.items.moments

import io.github.xchat.features.api.core.WeServiceApi
import io.github.xchat.features.api.core.models.MessageType
import io.github.xchat.features.api.ui.WeChatMessageContextMenuApi
import io.github.xchat.features.api.ui.WeMomentsApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.utils.CameraIcon

@Suppress("DEPRECATION")
@Feature(name = "消息转圈", categories = ["朋友圈"], description = "将一些简单的消息转发到朋友圈")
object ForwardMessagesToMoments : SwitchFeature(), WeChatMessageContextMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    private val SUPPORTED_MSG_TYPES = setOf(
        MessageType.TEXT, MessageType.QUOTE, MessageType.IMAGE, MessageType.VIDEO
    )

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            WeChatMessageContextMenuApi.MenuItem(
                777009, "转圈", CameraIcon,
                shouldShow = { it.type in SUPPORTED_MSG_TYPES },
                onClick = { _, chattingContext, msgInfo ->
                    val activity = chattingContext.activity

                    when (msgInfo.type) {
                        MessageType.TEXT -> {
                            WeMomentsApi.sendTextInUi(activity, msgInfo.actualContent)
                        }

                        MessageType.QUOTE -> {
                            WeMomentsApi.sendTextInUi(activity, msgInfo.quoteMsgActualContent!!)
                        }

                        MessageType.IMAGE -> {
                            WeMomentsApi.sendImagesInUi(activity, listOf(WeServiceApi.getImageMd5FromMsgInfo(msgInfo)))
                        }

                        MessageType.VIDEO -> {
                            val mp4Path = WeServiceApi.getVideoMp4PathFromMsgInfo(msgInfo)
                            WeMomentsApi.sendVideoInUi(activity, mp4Path)
                        }

                        else -> {}
                    }
                }
            )
        )
    }
}
