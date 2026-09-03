package io.github.xchat.features.items.home_screen_menu

import de.robv.android.xposed.XC_MethodHook
import io.github.xchat.features.api.core.WeConversationApi
import io.github.xchat.features.api.ui.WeHomeScreenPopupMenuApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.utils.VisibilityIcon
import io.github.xchat.ui.utils.VisibilityOffIcon

@Feature(name = "显隐全部对话", categories = ["首页右上角菜单"], description = "在首页右上角菜单添加菜单项, 可显示或隐藏全部对话")
object ToggleAllConversationsVisibility : SwitchFeature(), WeHomeScreenPopupMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeHomeScreenPopupMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeHomeScreenPopupMenuApi.removeProvider(this)
    }

    override fun getMenuItems(param: XC_MethodHook.MethodHookParam): List<WeHomeScreenPopupMenuApi.MenuItem> {
        return listOf(
            WeHomeScreenPopupMenuApi.MenuItem(
                777010, "显示对话", VisibilityIcon
            ) {
                WeConversationApi.setAllConversationVisibility(true)
            },
            WeHomeScreenPopupMenuApi.MenuItem(
                777011, "隐藏对话", VisibilityOffIcon
            ) {
                WeConversationApi.setAllConversationVisibility(false)
            },
        )
    }
}
