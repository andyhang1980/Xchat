package io.github.xchat.features.items.chat

import dev.ujhhgtg.comptime.This
import io.github.xchat.features.api.core.WeDatabaseApi
import io.github.xchat.features.api.core.WeMessageApi
import io.github.xchat.features.api.core.models.MessageInfo
import io.github.xchat.features.api.core.models.MessageType
import io.github.xchat.features.api.ui.WeChatMessageContextMenuApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.content.ContactsSelector
import io.github.xchat.ui.utils.ForwardIcon
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.AudioUtils
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.showToast
import io.github.xchat.utils.android.showToastSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Feature(
    name = "转发语音",
    categories = ["聊天"],
    description = "长按语音消息时添加「转发语音」选项, 可向好友或群聊转发语音"
)
object ForwardVoice : SwitchFeature(),
    WeChatMessageContextMenuApi.IMenuItemsProvider {

    private val TAG = This.Class.simpleName

    override fun onEnable() {
        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            WeChatMessageContextMenuApi.MenuItem(
                777020,
                "转发语音",
                ForwardIcon,
                shouldShow = { msgInfo -> msgInfo.type == MessageType.VOICE }
            ) { view, _, msgInfo ->
                CoroutineScope(Dispatchers.IO).launch {
                    val contacts = WeDatabaseApi.getFriends() + WeDatabaseApi.getGroups()

                    withContext(Dispatchers.Main) {
                        showComposeDialog(view.context) {
                            ContactsSelector(
                                title = "选择转发对象",
                                contacts = contacts,
                                initialSelectedWxIds = emptySet(),
                                onDismiss = onDismiss,
                                onConfirm = { selectedWxIds ->
                                    if (selectedWxIds.isEmpty()) {
                                        showToast("请选择至少一个联系人")
                                        return@ContactsSelector
                                    }

                                    onDismiss()
                                    forwardVoiceToContacts(msgInfo, selectedWxIds)
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun forwardVoiceToContacts(msgInfo: MessageInfo, wxIds: Set<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            showToastSuspend("正在转发到 ${wxIds.size} 个对象...")

            val encPath = msgInfo.imagePath
            if (encPath == null) {
                showToastSuspend("转发失败: 无法获取语音文件路径")
                return@launch
            }

            val voicePath = WeMessageApi.getVoiceFullPath(encPath)
            val durationMs = AudioUtils.getDurationMs(voicePath).toInt()

            if (durationMs <= 0) {
                showToastSuspend("转发失败: 无法获取语音时长")
                return@launch
            }

            var success = 0
            wxIds.forEach { wxId ->
                try {
                    if (WeMessageApi.sendVoice(wxId, voicePath, durationMs)) {
                        success++
                    }
                } catch (e: Exception) {
                    WeLogger.e(TAG, "failed to forward voice to $wxId", e)
                }
            }

            showToastSuspend(
                if (success == wxIds.size) "已转发到 ${wxIds.size} 个对象"
                else "已转发到 $success/${wxIds.size} 个对象"
            )
        }
    }
}
