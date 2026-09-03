package io.github.xchat.features.items.contacts

import android.app.Activity
import dev.ujhhgtg.reflekt.utils.toClassOrNull
import dev.ujhhgtg.comptime.nameOf
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.WeLogger

@Feature(name = "移除消息批量转发限制", categories = ["联系人与群组"], description = "移除消息多选目标的 9 个数量限制")
object RemoveMessageBatchForwardLimit : SwitchFeature() {

    private val TAG = nameOf(RemoveMessageBatchForwardLimit)

    override fun onEnable() {
        listOf(
            "com.tencent.mm.ui.mvvm.MvvmSelectContactUI",
            "com.tencent.mm.ui.mvvm.MvvmContactListUI"
        ).forEach {
            it.toClassOrNull()?.hookBeforeOnCreate {
                val activity = thisObject as Activity
                activity.intent.putExtra("max_limit_num", 999)
                WeLogger.i(TAG, "removed batch forward limit for $it")
            }
        }
    }
}
