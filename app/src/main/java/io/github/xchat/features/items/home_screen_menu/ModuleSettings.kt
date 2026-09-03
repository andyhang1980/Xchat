package io.github.xchat.features.items.home_screen_menu

import com.tencent.mm.ui.LauncherUI
import de.robv.android.xposed.XC_MethodHook
import io.github.xchat.BuildConfig
import io.github.xchat.features.api.ui.WeHomeScreenPopupMenuApi
import io.github.xchat.features.api.ui.WeSettingsInjector
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.utils.ExtensionIcon

@Feature(name = "模块设置", categories = ["首页右上角菜单"], description = "在首页右上角菜单添加「Xchat」选项")
object ModuleSettings : SwitchFeature(), WeHomeScreenPopupMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeHomeScreenPopupMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeHomeScreenPopupMenuApi.removeProvider(this)
    }

    override fun getMenuItems(param: XC_MethodHook.MethodHookParam): List<WeHomeScreenPopupMenuApi.MenuItem> =
        listOf(
            WeHomeScreenPopupMenuApi.MenuItem(
                0, BuildConfig.TAG, ExtensionIcon
            ) { WeSettingsInjector.openSettingsDialog(LauncherUI.getInstance()!!) }
        )
}
