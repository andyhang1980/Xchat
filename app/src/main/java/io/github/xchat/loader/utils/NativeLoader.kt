package io.github.xchat.loader.utils

import android.content.Context
import com.tencent.mmkv.MMKV
import io.github.xchat.preferences.WePrefs
import io.github.xchat.utils.fs.createDirsSafe
import kotlin.io.path.div
import kotlin.io.path.exists

object NativeLoader {

    init {
        System.loadLibrary("dexkit")
        System.loadLibrary("xchat_native")
    }

    fun init(hostCtx: Context) {
        val mmkvDir = hostCtx.filesDir.toPath() / "mmkv"
        if (!mmkvDir.exists()) {
            mmkvDir.createDirsSafe()
        }

        MMKV.initialize(hostCtx, mmkvDir.toString())

        MMKV.mmkvWithID(WePrefs.PREFS_NAME, MMKV.MULTI_PROCESS_MODE)
    }
}
