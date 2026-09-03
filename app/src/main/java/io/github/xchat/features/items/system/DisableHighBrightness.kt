package io.github.xchat.features.items.system

import android.view.WindowManager
import com.android.internal.policy.PhoneWindow
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import dev.ujhhgtg.reflekt.reflekt

@Feature(name = "禁止屏幕高亮度", categories = ["系统与隐私"], description = "禁止微信将屏幕亮度设置得过高")
object DisableHighBrightness : SwitchFeature() {

    override fun onEnable() {
        PhoneWindow::class.reflekt()
            .firstMethod {
                name = "setAttributes"
                parameters(WindowManager.LayoutParams::class)
            }
            .hookBefore {
                val lp = args[0] as WindowManager.LayoutParams
                if (lp.screenBrightness >= 0.5f) {
                    lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
    }
}
