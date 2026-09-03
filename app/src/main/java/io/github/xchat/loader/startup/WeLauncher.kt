package io.github.xchat.loader.startup

import android.content.Context
import android.content.res.Resources
import com.tencent.mm.boot.BuildConfig
import dev.ujhhgtg.comptime.This
import io.github.xchat.constants.PackageNames
import io.github.xchat.constants.Preferences
import io.github.xchat.dexkit.cache.DexCacheManager
import io.github.xchat.features.core.FeaturesLoader
import io.github.xchat.loader.utils.ActivityProxy
import io.github.xchat.loader.utils.ParcelableFixer
import io.github.xchat.utils.HostInfo
import io.github.xchat.utils.RuntimeConfig
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.hookBeforeDirectly
import io.github.xchat.utils.invokeOriginal
import io.github.xchat.utils.reflection.int

object WeLauncher {

    fun init(context: Context) {
        WeLogger.d(TAG, "loading in process name=${TargetProcesses.currentName}, type=${TargetProcesses.currentType}")

        ParcelableFixer.init()

        DexCacheManager.init(
            if (!Preferences.resetDexCacheOnHotUpdate) "${HostInfo.versionName}${HostInfo.versionCode}"
            else "${BuildConfig.VERSION_NAME}${BuildConfig.VERSION_CODE}${BuildConfig.CLIENT_VERSION_ARM64}"
        )

        if (TargetProcesses.isInMain) {
            val appContext = context.applicationContext ?: context
            ActivityProxy.init(appContext)

            val prefs =
                context.getSharedPreferences("${PackageNames.WECHAT}_preferences", Context.MODE_PRIVATE)
            RuntimeConfig.mmPrefs = prefs

            // fix up Jetpack Compose
            Resources::class.java.getDeclaredMethod("getString", int).hookBeforeDirectly {
                result = runCatching { invokeOriginal() }.getOrNull() ?: "null"
            }
        }

        runCatching {
            FeaturesLoader.loadFeatures()
//            val exportJson = run {
//                val map = WePrefs.default.getAll()
//                val jsonObject = buildJsonObject {
//                    for ((key, value) in map) {
//                        when (value) {
//                            is Boolean -> put(key, value)
//                            is Int -> put(key, value)
//                            is Long -> put(key, value)
//                            is Float -> put(key, value)
//                            is Double -> put(key, value)
//                            is String -> put(key, value)
//                            is Set<*> -> put(key, buildJsonArray {
//                                @Suppress("UNCHECKED_CAST")
//                                (value as Set<String>).forEach { add(it) }
//                            })
//                            null -> put(key, JsonNull)
//                        }
//                    }
//                }
//                DefaultJson.encodeToString(jsonObject)
//            }
//            WeLogger.d(TAG, "prefs:\n${exportJson}")
        }.onFailure { WeLogger.e(TAG, "failed to load hooks", it) }
    }

    private val TAG = This.Class.simpleName
}
