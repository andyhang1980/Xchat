package io.github.xchat.loader.startup

import android.app.Application
import android.content.Context
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.toClass
import io.github.xchat.loader.abc.IHookBridge
import io.github.xchat.loader.abc.ILoaderService
import io.github.xchat.loader.utils.HybridClassLoader
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.hookAfterDirectly
import io.github.xchat.utils.reflection.ClassLoaders

object UnifiedEntryPoint {

    private val TAG = This.Class.simpleName

    fun entry(
        loaderService: ILoaderService,
        hookBridge: IHookBridge?,
        initialClassLoader: ClassLoader,
        modulePath: String
    ) {
        val self = ClassLoaders.MODULE
        val selfParent = self.parent
        HybridClassLoader.moduleParentClassLoader = selfParent
        self.reflekt()
            .firstField { name = "parent"; superclass() }
            .set(HybridClassLoader)

        WeLogger.d(TAG, "hooking Application.attachBaseContext")

        "com.tencent.mm.app.Application".toClass(initialClassLoader).reflekt()
            .firstMethod { name = "attachBaseContext" }
            .hookAfterDirectly {
                WeLogger.d(TAG, "Application.attachBaseContext invoked, hooking Instrumentation.callApplicationOnCreate")
                val currentClassLoader = (thisObject as Context).classLoader
                "android.app.Instrumentation".toClass(currentClassLoader).reflekt()
                    .firstMethod("callApplicationOnCreate").hookAfterDirectly {
                        WeLogger.d(TAG, "Instrumentation.callApplicationOnCreate invoked, running StartupAgent")
                        runCatching {
                            StartupAgent.startup(
                                loaderService,
                                hookBridge,
                                modulePath,
                                args[0] as Application
                            )
                        }.onFailure { WeLogger.e(TAG, "StartupAgent failed", it) }
                    }
            }
    }
}
