package io.github.xchat.features.items.profile

import android.graphics.Bitmap
import dev.ujhhgtg.comptime.This
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger

@Feature(name = "上传透明头像", categories = ["个人资料"], description = "头像上传时使用 PNG 格式保持透明")
object UploadTransparentAvatars : SwitchFeature(), IResolveDex {

    private val TAG = This.Class.simpleName

    private val methodSaveBitmap by dexMethod {
        searchPackages("com.tencent.mm.sdk.platformtools")
        matcher {
            usingStrings("saveBitmapToImage pathName null or nil", "MicroMsg.BitmapUtil")
        }
    }

    override fun onEnable() {
        methodSaveBitmap.hookBefore {
            val args = args

            val pathName = args[3] as? String
            if (pathName != null &&
                (pathName.contains("avatar") || pathName.contains("user_hd"))
            ) {
                WeLogger.i(TAG, "检测到头像保存: $pathName")
                args[2] = Bitmap.CompressFormat.PNG
                WeLogger.i(TAG, "已将头像格式修改为PNG，保留透明通道")
            }
        }
    }
}
