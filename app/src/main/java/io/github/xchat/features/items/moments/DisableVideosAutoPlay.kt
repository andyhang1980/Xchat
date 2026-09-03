package io.github.xchat.features.items.moments

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature

@Feature(
    name = "禁止自动播放视频",
    categories = ["朋友圈"],
    description = "禁止朋友圈中的视频自动播放"
)
object DisableVideosAutoPlay : SwitchFeature(), IResolveDex {

    private val methodCheckAutoPlay by dexMethod {
        matcher {
            usingEqStrings(
                "checkAutoPlay",
                "com.tencent.mm.plugin.sns.util.SnsAutoPlayUtil"
            )
        }
    }

    private val methodImproveAutoPlayInvoke by dexMethod {
        matcher {
            usingEqStrings(
                "invoke",
                $$"com.tencent.mm.plugin.sns.ui.improve.util.ImproveAutoPlayManager$autoPlay$2"
            )
        }
    }

    override fun onEnable() {
        methodCheckAutoPlay.hookBefore {
            result = false
        }
        methodImproveAutoPlayInvoke.hookBefore {
            result = false
        }
    }
}
