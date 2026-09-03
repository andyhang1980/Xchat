package io.github.xchat.features.items.system

import android.annotation.SuppressLint
import android.content.ComponentName
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.reflekt
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.HostInfo
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.setEnabled
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

@Feature(name = "禁用微信热更新", categories = ["系统与隐私"], description = "禁止微信热更新, 避免被强制更新到不兼容版本")
object DisableHostHotUpdates : SwitchFeature() {

    private val componentNames = listOf(
        "com.tencent.tinker.lib.service.TinkerPatchForeService",
        "com.tencent.tinker.lib.service.TinkerPatchService",
        $$"com.tencent.tinker.lib.service.TinkerPatchService$InnerService",
        "com.tencent.tinker.lib.service.DefaultTinkerResultService",
    )

    @SuppressLint("SdCardPath")
    @OptIn(ExperimentalPathApi::class)
    override fun onEnable() {
        runCatching { Path("/data/data/${HostInfo.packageName}/tinker").deleteRecursively() }

        ShareTinkerInternals::class.reflekt()
            .methods {
                name {
                    it.startsWith("isTinkerEnabled")
                }
            }
            .forEach {
                it.hookBefore {
                    result = false
                }
            }

        batchSetEnabled(false)
    }

    override fun onDisable() {
        batchSetEnabled(true)
    }

    private fun batchSetEnabled(enabled: Boolean) {
        HostInfo.application.apply {
            componentNames.forEach {
                runCatching {
                    ComponentName(
                        this,
                        it
                    ).setEnabled(this, enabled)
                }.onFailure { WeLogger.e(TAG, "failed to set $enabled state for $it") }
            }
        }
    }

    private val TAG = This.Class.simpleName
}
