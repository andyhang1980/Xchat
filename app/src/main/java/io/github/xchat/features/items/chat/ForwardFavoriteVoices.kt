package io.github.xchat.features.items.chat

import android.app.Activity
import android.view.View
import androidx.compose.material3.Text
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.toClass
import io.github.xchat.features.api.core.WeMessageApi
import io.github.xchat.features.api.net.models.protobuf.FavInfoProto
import io.github.xchat.features.api.ui.WeCurrentConversationApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.AudioUtils
import io.github.xchat.utils.RuntimeConfig
import io.github.xchat.utils.android.copyToClipboard
import io.github.xchat.utils.android.getTopMostActivity
import io.github.xchat.utils.android.showToast
import io.github.xchat.utils.coerceToInt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

@Feature(name = "转发收藏语音", categories = ["聊天"], description = "在聊天菜单的「收藏」中允许转发语音")
object ForwardFavoriteVoices : SwitchFeature() {

    @OptIn(ExperimentalSerializationApi::class)
    override fun onEnable() {
        "com.tencent.mm.plugin.fav.ui.FavSelectUI".toClass().reflekt().firstMethod { name = "onItemClick" }.hookBefore {
            val view = args[1] as View

            val tag = view.tag

            val a = tag.reflekt().firstField { name = "a"; superclass() }.get()!!

            val type = a.reflekt().firstField { name = "field_type"; superclass() }.get()!! as Int

            if (type != 3) return@hookBefore

            val favPhoto = a.reflekt().firstField { name = "field_favProto"; superclass() }.get()!!
            val bytes = favPhoto.reflekt().firstMethod { name = "getData"; superclass() }.invoke()!! as ByteArray

            val favInfo = ProtoBuf.decodeFromByteArray<FavInfoProto>(bytes)
            val voiceInfo = favInfo.voiceInfo

            var voiceFilePath = voiceInfo.filePath

            if (voiceFilePath == null) {
                val baseStorageDir = RuntimeConfig.userDataDir
                val cacheName = voiceInfo.fileCacheName
                val bucketId = cacheName.hashCode() and 0xFF

                voiceFilePath = (baseStorageDir / "favorite" / bucketId.toString() / "$cacheName.${voiceInfo.fileCacheType}").absolutePathString()
            }

            val ctx = thisObject as Activity

            showComposeDialog(ctx) {
                AlertDialogContent(title = { Text("转发收藏语音") },
                    text = {
                        Text("确定发送以下文件?\n" +
                                voiceFilePath)
                    },
                    dismissButton = { TextButton(onDismiss) { Text("取消") } },
                    confirmButton = {
                        TextButton({
                            copyToClipboard(ctx, voiceFilePath)
                            showToast(ctx, "已复制")
                        }) { Text("复制路径") }
                        Button({
                            WeMessageApi.sendVoice(WeCurrentConversationApi.value, voiceFilePath, AudioUtils.getDurationMs(voiceFilePath).coerceToInt())
                            showToast(ctx, "已发送")
                            onDismiss()
                            getTopMostActivity()?.finish()
                        }) { Text("确定") }
                    })
            }

            result = null
        }
    }
}
