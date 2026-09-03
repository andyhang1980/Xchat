package io.github.xchat.features.items.moments

import com.tencent.mm.plugin.sns.storage.ADInfo
import dev.ujhhgtg.comptime.nameOf
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger
import dev.ujhhgtg.reflekt.reflekt

@Feature(name = "拦截朋友圈广告", categories = ["朋友圈"], description = "拦截朋友圈广告")
object RemoveMomentsAds : SwitchFeature() {

    private val TAG = nameOf(RemoveMomentsAds)

    override fun onEnable() {
        ADInfo::class.reflekt()
            .firstConstructor {
                parameters(String::class)
            }
            .hookBefore {
                WeLogger.i(TAG, "blocked ad")
                result = null
            }
    }
}
