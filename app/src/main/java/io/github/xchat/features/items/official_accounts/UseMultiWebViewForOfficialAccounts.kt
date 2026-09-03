package io.github.xchat.features.items.official_accounts

import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import dev.ujhhgtg.comptime.This
import io.github.xchat.features.api.ui.WeStartActivityApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger

@Feature(
    name = "允许公众号网页多开", categories = ["公众号"],
    description = "为公众号时间线预加载网页启用多任务"
)
object UseMultiWebViewForOfficialAccounts : SwitchFeature(), WeStartActivityApi.IStartActivityListener {

    private val tag = This.Class.simpleName

    override fun onEnable() {
        WeStartActivityApi.addListener(this)
    }

    override fun onDisable() {
        WeStartActivityApi.removeListener(this)
    }

    override fun onStartActivity(param: XC_MethodHook.MethodHookParam, intent: Intent) {
        val className = intent.component?.className ?: return
        if (!className.endsWith(".ui.timeline.preload.ui.TmplWebViewMMUI")) return

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        WeLogger.d(tag, "enabled multi webview for $className")
    }
}
