package io.github.xchat.features.items.voip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.preferences.WePrefs.Companion.prefOption
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.DefaultColumn
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog

@Feature(name = "屏蔽铃声", categories = ["聊天", "音视频通话"], description = "屏蔽音视频通话铃声")
object BlockVoipRingtone : ClickableFeature(), IResolveDex {

    private var disableOutCall by prefOption("voip_disable_ringtone_out_call", true)
    private var disableInCall by prefOption("voip_disable_ringtone_in_call", false)

    private val methodPlaySound by dexMethod {
        matcher {
            usingEqStrings("MicroMsg.BaseSceneSetting", "playSound Failed Throwable t = ")
        }
    }

    override fun onEnable() {
        methodPlaySound.hookBefore {
            val params = args[1] as? Bundle ?: return@hookBefore
            val scene = params.getString("scene") ?: return@hookBefore
            if (scene == "start") {
                val isOutCall = params.getBoolean("isOutCall")
                val disOutCall = isOutCall && disableOutCall
                val disInCall = !isOutCall && disableInCall
                if (disOutCall || disInCall) {
                    result = false
                }
            }
        }
    }

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var outCall by remember { mutableStateOf(disableOutCall) }
            var inCall by remember { mutableStateOf(disableInCall) }

            AlertDialogContent(
                title = { Text("屏蔽铃声") },
                text = {
                    DefaultColumn {
                        ListItem(
                            headlineContent = { Text("屏蔽呼出铃声") },
                            supportingContent = { Text("屏蔽拨出音视频通话时的铃声") },
                            trailingContent = { Switch(checked = outCall, onCheckedChange = { outCall = it }) },
                            modifier = Modifier.clickable { outCall = !outCall }
                        )
                        ListItem(
                            headlineContent = { Text("屏蔽呼入铃声") },
                            supportingContent = { Text("屏蔽收到音视频通话请求时的铃声") },
                            trailingContent = { Switch(checked = inCall, onCheckedChange = { inCall = it }) },
                            modifier = Modifier.clickable { inCall = !inCall }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        disableOutCall = outCall
                        disableInCall = inCall
                        onDismiss()
                    }) { Text("保存") }
                },
                dismissButton = { TextButton(onDismiss) { Text("取消") } }
            )
        }
    }
}
