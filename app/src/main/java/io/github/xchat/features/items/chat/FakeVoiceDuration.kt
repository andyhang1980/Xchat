package io.github.xchat.features.items.chat

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.preferences.WePrefs
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.android.showToast

@Feature(name = "伪装语音时长", categories = ["聊天"], description = "预设定伪装发送语音显示的时长")
object FakeVoiceDuration : ClickableFeature(), IResolveDex {

    private val methodVoiceRecorderGetLength by dexMethod {
        matcher {
            declaredClass {
                usingEqStrings("MicroMsg.SceneVoice.Recorder", "Stop file success: ")
            }
            returnType = "long"
        }
    }
    private const val KEY_DURATION = "fake_voice_duration"

    override fun onEnable() {
        methodVoiceRecorderGetLength.hookBefore {
            result = WePrefs.getLongOrDef(KEY_DURATION, 0L)
        }
    }

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var durationInput by remember { mutableStateOf(WePrefs.getLongOrDef(KEY_DURATION, 0).toString()) }
            AlertDialogContent(
                title = { Text("伪装语音时长") },
                text = {
                    TextField(
                        value = durationInput,
                        onValueChange = { durationInput = it.filter { c -> c.isDigit() } },
                        label = { Text("语音时长 (毫秒)") })
                },
                dismissButton = {
                    TextButton(onDismiss) { Text("取消") }
                },
                confirmButton = {
                    Button(onClick = {
                        val durationMs = durationInput.toLongOrNull()
                        if (durationMs == null) {
                            showToast("时长格式不正确!")
                            return@Button
                        }

                        WePrefs.putLong(KEY_DURATION, durationMs)
                        onDismiss()
                    }) { Text("确定") }
                })
        }
    }
}
