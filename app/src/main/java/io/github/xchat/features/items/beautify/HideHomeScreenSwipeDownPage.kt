package io.github.xchat.features.items.beautify

import android.view.View
import android.widget.AbsListView
import android.widget.ListView
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.features.items.chat.ConversationGrouping
import io.github.xchat.utils.invokeOriginal
import dev.ujhhgtg.reflekt.reflekt

@Feature(name = "隐藏主页下滑「最近」页", categories = ["界面美化"], description = "禁用主页下滑功能")
object HideHomeScreenSwipeDownPage : SwitchFeature() {

    override fun onEnable() {
        ListView::class.reflekt()
            .firstMethod {
                name = "addHeaderView"
                parameterCount = 3
            }
            .hookBefore {
                if (thisObject.javaClass.simpleName != "ConversationListView") return@hookBefore
                val view = args[0] as View
                val className = view.javaClass.simpleName
                if (className == "TaskBarContainer") {
                    val heightDp = if (!ConversationGrouping.isEnabled) 48 else 94
                    val heightPx = (heightDp * view.resources.displayMetrics.density).toInt()
                    val spacer = View(view.context).apply {
                        layoutParams = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, heightPx)
                    }
                    invokeOriginal(args = arrayOf(spacer, null, true))
                    result = null
                }
            }
    }
}
