package io.github.xchat.preferences

import android.content.SharedPreferences
import dev.ujhhgtg.comptime.nameOf
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.fs.KnownPaths
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.Executors
import kotlin.io.path.absolutePathString
import kotlin.io.path.div

class TursoPrefsImpl : WePrefs() {

    companion object {
        private const val TYPE_BOOL = 1
        private const val TYPE_INT = 2
        private const val TYPE_LONG = 3
        private const val TYPE_FLOAT = 4
        private const val TYPE_STRING = 5
        private const val TYPE_STRING_SET = 6
        private const val TYPE_BYTES = 7
        private const val TYPE_SERIALIZABLE = 8
    }

    private val cache = HashMap<String, Any>()
    private val writeExecutor = Executors.newSingleThreadExecutor { Thread(it, "turso-prefs-writer") }

    init {
        val dbPath = (KnownPaths.moduleData / "preferences.db").absolutePathString()
        nativeInit(dbPath)
        loadCache()
    }

    private fun loadCache() {
        val keys = nativeGetAllKeys()
        synchronized(cache) {
            for (key in keys) {
                val type = nativeGetType(key)
                if (type == 0) continue
                val value = getObjectByType(key, type)
                if (value != null) {
                    cache[key] = value
                }
            }
        }
    }

    // JNI Native methods
    private external fun nativeInit(dbPath: String)
    private external fun nativeGetString(key: String, defValue: String?): String?
    private external fun nativePutString(key: String, value: String?)
    private external fun nativeGetStringSet(key: String): Array<String>?
    private external fun nativePutStringSet(key: String, values: Array<String>)
    private external fun nativeGetInt(key: String, defValue: Int): Int
    private external fun nativePutInt(key: String, value: Int)
    private external fun nativeGetLong(key: String, defValue: Long): Long
    private external fun nativePutLong(key: String, value: Long)
    private external fun nativeGetFloat(key: String, defValue: Float): Float
    private external fun nativePutFloat(key: String, value: Float)
    private external fun nativeGetBoolean(key: String, defValue: Boolean): Boolean
    private external fun nativePutBoolean(key: String, value: Boolean)
    private external fun nativeGetBytes(key: String): ByteArray?
    private external fun nativePutBytesWithType(key: String, value: ByteArray, type: Int)
    private external fun nativeContains(key: String): Boolean
    private external fun nativeRemove(key: String)
    private external fun nativeClear()
    private external fun nativeGetAllKeys(): Array<String>
    private external fun nativeGetType(key: String): Int

    override fun getAll(): Map<String, *> {
        synchronized(cache) {
            return HashMap(cache)
        }
    }

    private fun getObjectByType(key: String, type: Int): Any? {
        return when (type) {
            TYPE_BOOL -> nativeGetBoolean(key, false)
            TYPE_INT -> nativeGetInt(key, 0)
            TYPE_LONG -> nativeGetLong(key, 0L)
            TYPE_FLOAT -> nativeGetFloat(key, 0f)
            TYPE_STRING -> nativeGetString(key, null)
            TYPE_STRING_SET -> nativeGetStringSet(key)?.toSet()
            TYPE_BYTES -> nativeGetBytes(key)
            TYPE_SERIALIZABLE -> {
                val bytes = nativeGetBytes(key) ?: return null
                runCatching {
                    ObjectInputStream(ByteArrayInputStream(bytes)).readObject()
                }.onFailure { WeLogger.e(nameOf(TursoPrefsImpl::class.java), "failed when getting Serializable object", it) }.getOrNull()
            }
            else -> null
        }
    }

    override fun getString(key: String?, defValue: String?): String? {
        if (key == null) return defValue
        synchronized(cache) {
            return cache[key] as? String ?: defValue
        }
    }

    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? {
        if (key == null) return defValues
        synchronized(cache) {
            @Suppress("UNCHECKED_CAST")
            return cache[key] as? Set<String> ?: defValues
        }
    }

    override fun getInt(key: String?, defValue: Int): Int {
        if (key == null) return defValue
        synchronized(cache) {
            return cache[key] as? Int ?: defValue
        }
    }

    override fun getLong(key: String?, defValue: Long): Long {
        if (key == null) return defValue
        synchronized(cache) {
            return cache[key] as? Long ?: defValue
        }
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        if (key == null) return defValue
        synchronized(cache) {
            return cache[key] as? Float ?: defValue
        }
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (key == null) return defValue
        synchronized(cache) {
            return cache[key] as? Boolean ?: defValue
        }
    }

    override fun contains(key: String?): Boolean {
        if (key == null) return false
        synchronized(cache) {
            return cache.containsKey(key)
        }
    }

    override fun getObject(key: String): Any? {
        synchronized(cache) {
            return cache[key]
        }
    }

    override fun getBytes(key: String, defValue: ByteArray?): ByteArray? {
        synchronized(cache) {
            return cache[key] as? ByteArray ?: defValue
        }
    }

    override fun getBytesOrDefault(key: String, defValue: ByteArray): ByteArray {
        synchronized(cache) {
            return cache[key] as? ByteArray ?: defValue
        }
    }

    override fun putBytes(key: String, value: ByteArray) {
        synchronized(cache) {
            cache[key] = value
        }
        writeExecutor.submit {
            nativePutBytesWithType(key, value, TYPE_BYTES)
        }
    }

    override fun save() {
        // Cached writes are submitted asynchronously. No extra work needed here.
    }

    override fun putObject(key: String, obj: Any): WePrefs {
        synchronized(cache) {
            cache[key] = obj
        }
        when (obj) {
            is Float, is Double -> {
                val f = (obj as Number).toFloat()
                writeExecutor.submit { nativePutFloat(key, f) }
            }
            is Long -> writeExecutor.submit { nativePutLong(key, obj) }
            is Int -> writeExecutor.submit { nativePutInt(key, obj) }
            is Boolean -> writeExecutor.submit { nativePutBoolean(key, obj) }
            is String -> writeExecutor.submit { nativePutString(key, obj) }
            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                val s = obj as Set<String>
                writeExecutor.submit { nativePutStringSet(key, s.toTypedArray()) }
            }
            is ByteArray -> writeExecutor.submit { nativePutBytesWithType(key, obj, TYPE_BYTES) }
            is Array<*> if obj.isArrayOf<String>() -> {
                @Suppress("UNCHECKED_CAST")
                val s = (obj as Array<String>).toHashSet()
                synchronized(cache) {
                    cache[key] = s
                }
                writeExecutor.submit { nativePutStringSet(key, s.toTypedArray()) }
            }
            is Serializable -> runCatching {
                val outputStream = ByteArrayOutputStream()
                ObjectOutputStream(outputStream).writeObject(obj)
                val bytes = outputStream.toByteArray()
                writeExecutor.submit { nativePutBytesWithType(key, bytes, TYPE_SERIALIZABLE) }
            }.onFailure { throw RuntimeException(it) }
            else -> throw IllegalArgumentException("unsupported type ${obj::class}")
        }
        return this
    }

    override fun putString(key: String?, value: String?): WePrefs {
        if (key != null) {
            synchronized(cache) {
                if (value == null) {
                    cache.remove(key)
                } else {
                    cache[key] = value
                }
            }
            writeExecutor.submit {
                nativePutString(key, value)
            }
        }
        return this
    }

    override fun putStringSet(key: String?, values: Set<String>?): WePrefs {
        if (key != null) {
            synchronized(cache) {
                if (values == null) {
                    cache.remove(key)
                } else {
                    cache[key] = values
                }
            }
            writeExecutor.submit {
                if (values == null) {
                    nativeRemove(key)
                } else {
                    nativePutStringSet(key, values.toTypedArray())
                }
            }
        }
        return this
    }

    override fun putInt(key: String?, value: Int): WePrefs {
        if (key != null) {
            synchronized(cache) {
                cache[key] = value
            }
            writeExecutor.submit {
                nativePutInt(key, value)
            }
        }
        return this
    }

    override fun putLong(key: String?, value: Long): WePrefs {
        if (key != null) {
            synchronized(cache) {
                cache[key] = value
            }
            writeExecutor.submit {
                nativePutLong(key, value)
            }
        }
        return this
    }

    override fun putFloat(key: String?, value: Float): WePrefs {
        if (key != null) {
            synchronized(cache) {
                cache[key] = value
            }
            writeExecutor.submit {
                nativePutFloat(key, value)
            }
        }
        return this
    }

    override fun putBoolean(key: String?, value: Boolean): WePrefs {
        if (key != null) {
            synchronized(cache) {
                cache[key] = value
            }
            writeExecutor.submit {
                nativePutBoolean(key, value)
            }
        }
        return this
    }

    override fun remove(key: String?): WePrefs {
        if (key != null) {
            synchronized(cache) {
                cache.remove(key)
            }
            writeExecutor.submit {
                nativeRemove(key)
            }
        }
        return this
    }

    override fun clear(): WePrefs {
        synchronized(cache) {
            cache.clear()
        }
        writeExecutor.submit {
            nativeClear()
        }
        return this
    }

    override fun commit(): Boolean {
        val future = writeExecutor.submit { /* no-op task to drain queue */ }
        return try {
            future.get()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun apply() = Unit

    override val isReadOnly: Boolean = false
    override val isPersistent: Boolean = true

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
}
