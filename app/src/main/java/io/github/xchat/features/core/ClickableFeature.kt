package io.github.xchat.features.core

import androidx.activity.ComponentActivity
import io.github.xchat.preferences.WePrefs
import io.github.xchat.utils.TargetProcesses

abstract class ClickableFeature : SwitchFeature() {

    override fun startup() {
        if (!TargetProcesses.isInMain) return
        _isEnabled = WePrefs.getBoolOrFalse(name)
        if (_isEnabled || alwaysEnabled) enable()
    }

    open val alwaysEnabled: Boolean = false

    open val noSwitchWidget = false

    abstract fun onClick(context: ComponentActivity)
}
