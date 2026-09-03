package io.github.xchat.utils

import android.content.Context
import dev.ujhhgtg.comptime.This
import io.github.xchat.constants.PackageNames

object SignatureVerifier {

    private val TAG = This.Class.simpleName

    private external fun nativeVerify(context: Context, packageName: String): Boolean

    fun verify(context: Context) {
        if (nativeVerify(context, PackageNames.MODULE)) {
            WeLogger.i(TAG, "signature verification succeeded")
            return
        }

        WeLogger.e(TAG, "signature verification failed")
        error("signature verification failed")
    }
}
