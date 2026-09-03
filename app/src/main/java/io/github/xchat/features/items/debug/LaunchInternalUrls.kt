package io.github.xchat.features.items.debug

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.api.core.WeUnsafeApi
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.DefaultColumn
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog

@Feature(name = "启动微信内部 URL", categories = ["调试"], description = "跳转微信 weixin:// URL")
object LaunchInternalUrls : ClickableFeature(), IResolveDex {

    override val noSwitchWidget = true

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var url by remember { mutableStateOf("weixin://") }
            var argsInput by remember { mutableStateOf("") }

            AlertDialogContent(
                title = { Text("启动微信内部 URL") },
                text = {
                    DefaultColumn {
                        TextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("URL") })
                        TextField(
                            value = argsInput,
                            onValueChange = { argsInput = it },
                            label = { Text("参数 (可留空)") })
                    }
                },
                dismissButton = { TextButton(onDismiss) { Text("取消") } },
                confirmButton = {
                    Button(onClick = {
                        onDismiss()
                        val args = if (argsInput.isBlank()) emptyList() else argsInput.split("\n")
                        methodOpenUrl.method.invoke(
                            // FIXME: getDeclaredConstructor() says no ctor exists?? but Unsafe works????
                            WeUnsafeApi.allocateInstance(methodOpenUrl.method.declaringClass),
                            *arrayOf(context, url, args.toTypedArray())
                        )
                    }) { Text("确定") }
                })
        }
    }

    private val methodOpenUrl by dexMethod {
        searchPackages("com.tencent.mm.app.plugin")
        matcher {
            usingEqStrings("MicroMsg.MMURIJumpHandler", "openSpecificUI, context is null")
        }
    }
}
