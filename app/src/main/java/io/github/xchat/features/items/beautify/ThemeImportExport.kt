package io.github.xchat.features.items.beautify

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import org.luckypray.dexkit.DexKitBridge

import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.preferences.WePrefs
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.features.core.Feature

@Feature(
    name = "主题导入导出",
    categories = ["界面美化"],
    description = "支持主题包的导入导出，方便分享和备份"
)
object ThemeImportExport : SwitchFeature(), IResolveDex {

    private const val TAG = "ThemeImportExport"

    private var themeJson by prefOption("tie_theme_json", "")

    private val methodExportTheme by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.Theme", "export theme")
        }
    }

    private val methodImportTheme by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.Theme", "import theme")
        }
    }

    override fun resolveDex(dexKit: DexKitBridge) {
        methodExportTheme.find(dexKit, allowFailure = true) {
            matcher { usingEqStrings("MicroMsg.Theme", "export theme") }
        }
        methodImportTheme.find(dexKit, allowFailure = true) {
            matcher { usingEqStrings("MicroMsg.Theme", "import theme") }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodExportTheme.isPlaceholder && methodImportTheme.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持主题导入导出")
            return
        }
        runCatching {
            methodExportTheme.hookBefore { WeLogger.d(TAG, "export theme") }
            methodImportTheme.hookBefore { WeLogger.d(TAG, "import theme") }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
