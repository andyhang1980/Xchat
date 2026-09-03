package io.github.xchat.features.core

import android.content.Context
import dev.ujhhgtg.comptime.nameOf
import io.github.xchat.preferences.WePrefs
import io.github.xchat.utils.TargetProcesses
import io.github.xchat.utils.WeLogger

abstract class SwitchFeature : BaseFeature() {

    override fun startup() {
        if (!TargetProcesses.isInMain) return
        _isEnabled = WePrefs.getBoolOrFalse(name)
        if (_isEnabled) enable()
    }

    @Suppress("PropertyName")
    protected var _isEnabled = false

    var isEnabled
        get() = _isEnabled
        set(value) {
            if (_isEnabled == value) return
            _isEnabled = value
            if (!value) {
                if (isLoaded) {
                    WeLogger.i(nameOf(SwitchFeature::class), "disabling $displayName...")
                    disable()
                    isLoaded = false
                }
            } else {
                WeLogger.i(nameOf(SwitchFeature::class), "enabling $displayName...")
                enable()
                isLoaded = true
            }
        }

    private var isLoaded: Boolean = false
    private var toggleCompletionCallback: Runnable? = null

    open fun onBeforeToggle(newState: Boolean, context: Context): Boolean = true

    fun setToggleCompletionCallback(callback: Runnable) {
        toggleCompletionCallback = callback
    }

    fun applyToggle(newState: Boolean) {
        WePrefs.putBool(name, newState)
        isEnabled = newState
        toggleCompletionCallback?.run()
    }
}
