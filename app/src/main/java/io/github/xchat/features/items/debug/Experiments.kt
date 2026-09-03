package io.github.xchat.features.items.debug

import androidx.activity.ComponentActivity
import dev.ujhhgtg.comptime.This
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature

@Feature(name = "测试", categories = ["调试"], description = "???")
object Experiments : ClickableFeature() {

    @Suppress("unused")
    private val TAG = This.Class.simpleName

    override val noSwitchWidget = true

    override fun onClick(context: ComponentActivity) {
    }
}
