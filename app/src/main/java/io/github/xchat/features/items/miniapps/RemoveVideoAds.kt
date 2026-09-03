package io.github.xchat.features.items.miniapps

import dev.ujhhgtg.reflekt.utils.toClass
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import dev.ujhhgtg.reflekt.reflekt
import org.json.JSONObject

@Feature(name = "移除视频广告", categories = ["小程序"], description = "跳过小程序视频广告")
object RemoveVideoAds : SwitchFeature() {

    override fun onEnable() {
        "com.tencent.mm.appbrand.commonjni.AppBrandJsBridgeBinding".toClass().reflekt()
            .firstMethod { name = "subscribeHandler" }
            .hookBefore {
                val type = args[0] as String? ?: ""
                val json = args[1] as String? ?: ""

                if (type == "onVideoTimeUpdate") {
                    val json = JSONObject(json)
                    json.put("position", 60)
                    json.put("duration", 1)
                    args[1] = json.toString()
                }
            }
    }
}
