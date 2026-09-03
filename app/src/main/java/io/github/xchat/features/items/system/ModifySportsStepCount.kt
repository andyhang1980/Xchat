package io.github.xchat.features.items.system

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.ujhhgtg.reflekt.utils.createInstance
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.android.showToast

@Feature(name = "修改运动步数", categories = ["系统与隐私"], description = "修改微信获取到的或手动上传运动步数")
object ModifySportsStepCount : ClickableFeature(), IResolveDex {

    private val methodGetSteps by dexMethod {
        searchPackages("com.tencent.mm.plugin.sport.model")
        matcher {
            usingEqStrings("MicroMsg.Sport.DeviceStepManager", "get today step from %s todayStep %d")
        }
    }
    private val methodUploadSteps by dexMethod {
        searchPackages("com.tencent.mm.plugin.sport.model")
        matcher {
            usingEqStrings("MicroMsg.Sport.DeviceStepManager", "update device Step time: %s stepCount: %s")
        }
    }

    override fun onEnable() {
        methodGetSteps.hookBefore {
            val count = stepCount
            if (count < 0) return@hookBefore
            result = count
        }
    }

    private var stepCount by prefOption("step_count", -1L)

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var stepCountInput by remember { mutableStateOf(stepCount.toString()) }

            AlertDialogContent(
                title = { Text(text = "修改运动步数") },
                text = {
                    TextField(
                        value = stepCountInput,
                        onValueChange = {
                            stepCountInput = it.filter { c -> c.isDigit() }.trim()
                        },
                        label = { Text("步数") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val count = stepCountInput.toLongOrNull() ?: run {
                            showToast("格式不正确!")
                            return@Button
                        }
                        stepCount = count
                        onDismiss()
                    }) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onDismiss) {
                        Text("取消")
                    }

                    TextButton(onClick = {
                        val count = stepCountInput.toLongOrNull() ?: run {
                            showToast("格式不正确!")
                            return@TextButton
                        }
                        val sportsMan = methodUploadSteps.method.declaringClass.createInstance()
                        val result = methodUploadSteps.method.invoke(sportsMan, count) as Boolean
                        showToast(context, "已上传! 返回结果: ${if (result) "成功" else "失败"}")
                    }) {
                        Text("立即上传")
                    }
                }
            )
        }
    }

    override fun onBeforeToggle(newState: Boolean, context: Context): Boolean {
        if (newState) {
            showComposeDialog(context) {
                AlertDialogContent(
                    title = { Text(text = "警告") },
                    text = { Text(text = "此功能可能导致账号异常, 确定要启用吗?") },
                    confirmButton = {
                        Button(onClick = {
                            applyToggle(true)
                            onDismiss()
                        }) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onDismiss) {
                            Text("取消")
                        }
                    }
                )
            }
            return false
        }

        return true
    }
}
