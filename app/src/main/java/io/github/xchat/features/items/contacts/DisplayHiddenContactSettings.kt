package io.github.xchat.features.items.contacts

import android.widget.BaseAdapter
import com.tencent.mm.plugin.profile.ui.ProfileSettingUI
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import dev.ujhhgtg.reflekt.reflekt

@Feature(name = "显示隐藏朋友设置项", categories = ["联系人与群组"], description = "阻止微信隐藏朋友设置; 部分设置项可能显示异常, 但不影响功能")
object DisplayHiddenContactSettings : SwitchFeature() {

    override fun onEnable() {
        ProfileSettingUI::class.reflekt()
            .firstMethod {
                name = "initView"
            }.hookAfter {
                val prefScreen = thisObject.reflekt()
                    .firstMethod {
                        name = "getPreferenceScreen"
                        superclass()
                    }.invoke()!!
                val hiddenSet = prefScreen.reflekt()
                    .firstField {
                        type = HashSet::class
                    }.get()!! as HashSet<*>
                hiddenSet.clear()
                (prefScreen as BaseAdapter).notifyDataSetChanged()
            }
    }
}
