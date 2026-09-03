package io.github.xchat.features.items.home_screen_menu

import de.robv.android.xposed.XC_MethodHook
import io.github.xchat.features.api.core.WeConversationApi
import io.github.xchat.features.api.ui.WeHomeScreenPopupMenuApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.utils.CheckCircleIcon
import io.github.xchat.utils.android.showToast

@Feature(name = "清空未读", categories = ["首页右上角菜单"], description = "在首页右上角菜单添加「清空未读」选项")
object MarkAllAsRead : SwitchFeature(), WeHomeScreenPopupMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeHomeScreenPopupMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeHomeScreenPopupMenuApi.removeProvider(this)
    }

    override fun getMenuItems(param: XC_MethodHook.MethodHookParam): List<WeHomeScreenPopupMenuApi.MenuItem> {
        return listOf(
            WeHomeScreenPopupMenuApi.MenuItem(
                777012, "清空未读", CheckCircleIcon
            ) {
                WeConversationApi.markAllAsRead()
                showToast("已将全部未读消息标为已读")
            }
        )
    }
}
