package io.github.xchat.utils

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.ujhhgtg.comptime.This
import io.github.xchat.constants.PackageNames

enum class HostSpecies { WeChat, Xchat, Unknown }

data class HostInfoImpl(
    val application: Application,
    val packageName: String,
    val hostName: String,
    val versionCode: Long,
    val versionName: String,
    val hostSpecies: HostSpecies
)

object HostInfo {

    private lateinit var _info: HostInfoImpl

    val info: HostInfoImpl get() = _info
    val application: Application get() = _info.application
    val appInfo: ApplicationInfo get() = application.applicationInfo
    val packageName: String get() = _info.packageName
    val versionCode: Long get() = _info.versionCode
    val versionName: String get() = _info.versionName
    val isModule: Boolean get() = _info.hostSpecies == HostSpecies.Xchat
    val isHost: Boolean get() = !isModule

    val isHostGooglePlay: Boolean by lazy {
        com.tencent.mm.boot.BuildConfig.BUILD_TAG.contains("GP", ignoreCase = true)
    }

    /**
     * WeChat version code constants for runtime feature compatibility.
     * Values based on the versionCode stored in PackageInfo.
     */
    object MMVersion {
        const val MM_8_0_65: Long = 30902000
        const val MM_8_0_70: Long = 31200000
        const val MM_8_0_71: Long = 31201000
        const val MM_8_0_76: Long = 31400000
        const val MM_8_0_77: Long = 31401000
        const val MM_8_0_78: Long = 31800000

        /** Current WeChat version code, fetched at runtime */
        val currentVersionCode: Long get() = HostInfo.versionCode

        fun isAtLeast(version: Long): Boolean = currentVersionCode >= version
        fun is8_0_77OrAbove(): Boolean = currentVersionCode >= MM_8_0_77
        fun is8_0_78OrAbove(): Boolean = currentVersionCode >= MM_8_0_78
    }

    /**
     * Check if the current WeChat version is 8.0.77 or above,
     * where several internal method signatures changed.
     */
    val isNewWeChatVersion: Boolean get() = MMVersion.is8_0_77OrAbove()

    fun init(application: Application) {
        check(!::_info.isInitialized) { "HostInfo has already been initialized" }

        val pm = application.packageManager
        val packageName = application.packageName
        val packageInfo = try {
            pm.getPackageInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            WeLogger.e(This.Class.simpleName, "failed to get package info", e)
            throw e
        }

        _info = HostInfoImpl(
            application = application,
            packageName = packageName,
            hostName = application.applicationInfo.loadLabel(pm).toString(),
            versionCode = packageInfo.longVersionCode,
            versionName = packageInfo.versionName.orEmpty(),
            hostSpecies = run {
                if (PackageNames.isWeChat(packageName)) return@run HostSpecies.WeChat
                return@run when (packageName) {
                    PackageNames.MODULE -> HostSpecies.Xchat
                    else -> HostSpecies.Unknown
                }
            }
        )
    }
}
