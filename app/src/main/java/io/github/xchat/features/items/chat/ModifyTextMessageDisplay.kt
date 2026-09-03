package io.github.xchat.features.items.chat

import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.ujhhgtg.reflekt.reflekt
import io.github.xchat.features.api.ui.WeChatMessageContextMenuApi
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.content.TextButton
import io.github.xchat.ui.utils.EditIcon
import io.github.xchat.ui.utils.showComposeDialog

@Feature(name = "修改文本消息显示", categories = ["聊天"], description = "向消息长按菜单添加菜单项, 可修改本地消息显示内容")
object ModifyTextMessageDisplay : SwitchFeature(),
    WeChatMessageContextMenuApi.IMenuItemsProvider {

    override fun onEnable() {
        WeChatMessageContextMenuApi.addProvider(this)
    }

    override fun onDisable() {
        WeChatMessageContextMenuApi.removeProvider(this)
    }

    override fun getMenuItems(): List<WeChatMessageContextMenuApi.MenuItem> {
        return listOf(
            WeChatMessageContextMenuApi.MenuItem(
                777002,
                "修改内容",
                EditIcon,
                { msgInfo -> msgInfo.type?.isText ?: false }
            ) { view, _, _ ->
                showComposeDialog(view.context) {
                    var input by remember {
                        mutableStateOf(
                            view.reflekt()
                                .firstField {
                                    type = CharSequence::class
                                    superclass()
                                }.get().toString()
                        )
                    }

                    AlertDialogContent(
                        title = { Text("修改消息显示") },
                        text = {
                            TextField(
                                value = input,
                                onValueChange = { input = it },
                                label = { Text("显示内容") })
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                view.reflekt()
                                    .firstMethod {
                                        parameters(CharSequence::class)
                                    }
                                    .invoke(input)
                                onDismiss()
                            }) {
                                Text("确定")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { onDismiss() }) {
                                Text("取消")
                            }
                        })
                }
            }
        )
    }
}
