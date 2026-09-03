package io.github.xchat.ui.utils

import android.content.Context
import android.content.ContextWrapper
import io.github.xchat.utils.reflection.ClassLoaders

class CommonContextWrapper private constructor(base: Context?) : ContextWrapper(base) {

    override fun getClassLoader(): ClassLoader {
        return ClassLoaders.MODULE
    }

    companion object {
        fun create(base: Context): Context {
            return CommonContextWrapper(base)
        }
    }
}
