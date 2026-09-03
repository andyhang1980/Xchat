package io.github.xchat.features.items.chat

import com.tencent.mm.plugin.gif.MMWXGFJNI
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.Modifiers
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexClass
import io.github.xchat.features.api.core.WeServiceApi
import io.github.xchat.features.api.ui.WeChatMessageContextMenuApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.utils.DownloadIcon
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.showToastSuspend
import io.github.xchat.utils.fs.KnownPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.path.div
import kotlin.io.path.outputStream

@Feature(name = "贴纸保存到本地", categories = ["聊天"], description = "在贴纸消息菜单添加保存按钮, 允许将图片保存到本地")
object SaveStickersToLocalStorage : SwitchFeature(), IResolveDex,
    WeChatMessageContextMenuApi.IMenuItemsProvider {

    private val TAG = This.Class.simpleName

    private val classEmojiFileEncryptMgr by dexClass {
        matcher {
            methods {
                add {
                    usingEqStrings(
                        "MicroMsg.emoji.EmojiFileEncryptMgr",
                        "decode emoji file failed. path is no exist :%s "
                    )
                }
            }
        }
    }

    override fun onEnable() {
        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            @Suppress("UNCHECKED_CAST")
            WeChatMessageContextMenuApi.MenuItem(
                777001,
                "存本地",
                DownloadIcon,
                { msgInfo -> msgInfo.type?.isSticker ?: false }
            ) { _, _, msgInfo ->
                val md5 = msgInfo.imagePath!!
                val emojiInfo = WeServiceApi.getEmojiInfoByMd5(md5)
                val emojiFileEncryptMgr = classEmojiFileEncryptMgr.reflekt()
                    .firstMethod {
                        modifiers(Modifiers.STATIC)
                        parameterCount = 0
                    }
                    .invokeStatic()!!
                var bytes = emojiFileEncryptMgr.reflekt()
                    .firstMethod {
                        parameters("com.tencent.mm.api.IEmojiInfo")
                        returnType = ByteArray::class
                    }
                    .invoke(emojiInfo) as ByteArray
                bytes = MMWXGFJNI.nativeWxamToGif(bytes)

                val fileName = "sticker_${System.currentTimeMillis()}.gif"
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        (KnownPaths.downloads / fileName).outputStream().use { out ->
                            out.write(bytes)
                        }
                    }.onSuccess {
                        showToastSuspend("已将贴纸保存到 /sdcard/Download/Xchat/$fileName")
                    }.onFailure { e ->
                        WeLogger.e(TAG, "failed to save sticker $fileName", e)
                        showToastSuspend("贴纸保存失败! 查看日志以了解错误详情")
                    }
                }
            }
        )
    }
}
