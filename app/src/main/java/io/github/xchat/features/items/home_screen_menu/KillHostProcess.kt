package io.github.xchat.features.items.home_screen_menu

import de.robv.android.xposed.XC_MethodHook
import io.github.xchat.features.api.ui.WeHomeScreenPopupMenuApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.utils.CancelIcon
import io.github.xchat.utils.killHost

@Feature(name = "强行停止", categories = ["首页右上角菜单"], description = "在首页右上角菜单添加「强行停止」选项")
object KillHostProcess : SwitchFeature(), WeHomeScreenPopupMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeHomeScreenPopupMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeHomeScreenPopupMenuApi.removeProvider(this)
    }

    override fun getMenuItems(param: XC_MethodHook.MethodHookParam): List<WeHomeScreenPopupMenuApi.MenuItem> {
        return listOf(
            WeHomeScreenPopupMenuApi.MenuItem(
                777015, "强行停止", CancelIcon
            ) {
                killHost()
            }
        )
    }
}
