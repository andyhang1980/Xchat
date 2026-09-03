package io.github.xchat.loader.utils

import dev.ujhhgtg.comptime.This
import io.github.xchat.utils.reflection.ClassLoaders

object HybridClassLoader : ClassLoader(ClassLoaders.BOOT) {

    private val TAG = This.Class.simpleName

    private val bootClassLoader = ClassLoaders.BOOT
    lateinit var moduleParentClassLoader: ClassLoader
    lateinit var hostClassLoader: ClassLoader

//    @Volatile
//    var prioritizeHostClasses = false

    override fun findClass(name: String): Class<*> {
        try {
            return bootClassLoader.loadClass(name)
        } catch (_: ClassNotFoundException) {
        }

//        if (!prioritizeHostClasses) {
            try {
                return moduleParentClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {
            }

            try {
                return hostClassLoader.loadClass(name)
            }
            catch (_: UninitializedPropertyAccessException) { }
            catch (_: ClassNotFoundException) { }
//        }
//        else {
//            try {
//                return hostClassLoader.loadClass(name)
//            } catch (_: ClassNotFoundException) {
//            }
//
//            try {
//                return moduleParentClassLoader.loadClass(name)
//            } catch (_: ClassNotFoundException) {
//            }
//        }

        throw ClassNotFoundException(name)
    }
}
