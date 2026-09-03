package io.github.xchat.features.items.official_accounts

import android.content.ComponentName
import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import dev.ujhhgtg.comptime.This
import io.github.xchat.constants.PackageNames
import io.github.xchat.features.api.ui.WeStartActivityApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.HostInfo
import io.github.xchat.utils.WeLogger

@Feature(name = "恢复旧版公众号列表", categories = ["公众号"], description = "!!! 仅适用于旧版本微信 !!!\n新版本已在代码中移除旧 UI, 无法继续使用本功能")
object UseLegacyOfficialAccountsView : SwitchFeature(), WeStartActivityApi.IStartActivityListener {

    override fun onEnable() {
        WeStartActivityApi.addListener(this)
    }

    override fun onDisable() {
        WeStartActivityApi.removeListener(this)
    }

    override fun onStartActivity(param: XC_MethodHook.MethodHookParam, intent: Intent) {
        val className = intent.component?.className
        if (className == "${PackageNames.WECHAT}.plugin.brandservice.ui.flutter.BizFlutterTLFlutterViewActivity" ||
            className == "${PackageNames.WECHAT}.plugin.brandservice.ui.timeline.BizTimeLineUI"
        ) {
            WeLogger.d(This.Class.simpleName, "redirected $className")
            intent.component = ComponentName(
                HostInfo.packageName,
                "${PackageNames.WECHAT}.ui.conversation.NewBizConversationUI"
            )
        }
    }
}
