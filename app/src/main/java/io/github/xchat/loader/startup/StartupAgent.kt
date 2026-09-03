package io.github.xchat.loader.startup

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import dev.ujhhgtg.comptime.This
import dev.ujhhgtg.reflekt.utils.ReflectionClassLoader
import io.github.xchat.loader.abc.IHookBridge
import io.github.xchat.loader.abc.ILoaderService
import io.github.xchat.loader.utils.HybridClassLoader
import io.github.xchat.loader.utils.NativeLoader
import io.github.xchat.utils.HostInfo
import io.github.xchat.utils.WeLogger
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.lang.reflect.Field
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

object StartupAgent {

    private val TAG = This.Class.simpleName

    private var initialized = false

    @OptIn(ExperimentalPathApi::class)
    fun startup(
        loaderService: ILoaderService,
        hookBridge: IHookBridge?,
        modulePath: String,
        application: Application
    ) {
        if (initialized) return
        initialized = true

        val realClassLoader = application.baseContext.classLoader
        HybridClassLoader.hostClassLoader = realClassLoader
        ReflectionClassLoader.value = realClassLoader
        StartupInfo.loaderService = loaderService
        StartupInfo.hookBridge = hookBridge

        ensureHiddenApiAccess()
        checkWriteXorExecuteForModulePath(modulePath)

        HostInfo.init(application)
        NativeLoader.init(application)
        // FIXME: some people have hiding on, which causes false positives in signature verifier
//        SignatureVerifier.verify(application)
        WeLauncher.init(application)

        runCatching {
            application.dataDir.toPath().resolve("app_qqprotect").deleteRecursively()
        }.onFailure { WeLogger.e(TAG, "failed to delete app_qqprotect", it) }
    }

    private fun checkWriteXorExecuteForModulePath(modulePath: String) {
        val moduleFile = File(modulePath)
        if (moduleFile.canWrite()) {
            WeLogger.w(TAG, "module path is writable: $modulePath\nThis may cause issues on Android 15+, please check your Xposed framework")
        }
    }

    private fun ensureHiddenApiAccess() {
        if (!isHiddenApiAccessible()) {
            WeLogger.w(
                TAG,
                "hidden api is not accessible, SDK_INT is ${Build.VERSION.SDK_INT}"
            )
            HiddenApiBypass.setHiddenApiExemptions("L")
        }
    }

    @SuppressLint("BlockedPrivateApi", "PrivateApi")
    fun isHiddenApiAccessible(): Boolean {
        val kContextImpl = runCatching {
            Class.forName("android.app.ContextImpl")
        }.getOrElse { return false }

        var mActivityToken: Field? = null
        var mToken: Field? = null

        try {
            mActivityToken = kContextImpl.getDeclaredField("mActivityToken")
        } catch (_: NoSuchFieldException) {
        }
        try {
            mToken = kContextImpl.getDeclaredField("mToken")
        } catch (_: NoSuchFieldException) {
        }

        return mActivityToken != null || mToken != null
    }
}
