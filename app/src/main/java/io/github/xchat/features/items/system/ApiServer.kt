package io.github.xchat.features.items.system

import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.features.core.Feature

@Feature(
    name = "API 服务器",
    categories = ["系统与工具"],
    description = "提供 HTTP API 接口，可对接外部应用"
)
object ApiServer : SwitchFeature(), IResolveDex {

    private const val TAG = "ApiServer"

    private val methodApiHandler by dexMethod(allowFailure = true) {
        matcher {
            usingEqStrings("MicroMsg.API.Handler", "handle api request")
        }
    }

    override fun resolveDex(dexKit: io.github.xchat.dexkit.DexKitBridge) {
        methodApiHandler.find(dexKit, allowFailure = true) {
            matcher {
                usingEqStrings("MicroMsg.API.Handler", "handle api request")
            }
        }
    }

    override fun onEnable() {
        if (TargetProcesses.isInMain.not()) return
        if (methodApiHandler.isPlaceholder) {
            WeLogger.w(TAG, "当前微信版本不支持API服务器")
            return
        }
        runCatching {
            methodApiHandler.hookBefore {
                WeLogger.d(TAG, "api server request received")
            }
        }.onFailure { e -> WeLogger.e(TAG, "hook failed", e) }
    }
}
