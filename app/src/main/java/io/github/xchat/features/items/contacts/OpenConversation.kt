package io.github.xchat.features.items.contacts

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.xchat.features.api.core.WeApi
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.Button
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.android.showToast

@Feature(name = "跳转对话", categories = ["联系人与群组"], description = "打开指定微信 ID 的对话/好友主页/好友设置界面")
object OpenConversation : ClickableFeature() {

    override val noSwitchWidget = true

    override fun onClick(context: ComponentActivity) {
        showComposeDialog(context) {
            var wxId by remember { mutableStateOf("") }
            AlertDialogContent(
                title = { Text("跳转对话") },
                text = {
                    TextField(
                        value = wxId,
                        onValueChange = { wxId = it },
                        label = { Text("微信 ID") })
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (wxId.isBlank()) {
                            showToast(context, "微信 ID 为空!")
                            return@TextButton
                        }
                        WeApi.openContact(context, wxId, WeApi.OpenContactDestination.HOMEPAGE)
                    }) { Text("好友主页") }

                    TextButton(onClick = {
                        if (wxId.isBlank()) {
                            showToast(context, "微信 ID 为空!")
                            return@TextButton
                        }
                        WeApi.openContact(context, wxId, WeApi.OpenContactDestination.SETTINGS)
                    }) { Text("好友设置") }

                    Button(onClick = {
                        if (wxId.isBlank()) {
                            showToast(context, "微信 ID 为空!")
                            return@Button
                        }
                        WeApi.openContact(context, wxId, WeApi.OpenContactDestination.CONVERSATION)
                    }) { Text("对话") }
                })
        }
    }
}
