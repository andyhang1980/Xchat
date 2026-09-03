package io.github.xchat.features.items.scripting_java

import androidx.activity.ComponentActivity
import io.github.xchat.activity.TransparentActivity
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.utils.registerBshSnapshotDecompileLaunchers

@Feature(name = "反编译 BeanShell 快照", categories = ["脚本 (Java)"], description = "不知道这是干啥的就别管了")
object DecompileBeanShellSnapshot : ClickableFeature() {

    override val noSwitchWidget = true

    override fun onClick(context: ComponentActivity) {
        TransparentActivity.launch(context) {
            val selectFileLauncher = registerBshSnapshotDecompileLaunchers { finish() }
            selectFileLauncher.launch("*/*")
        }
    }
}
