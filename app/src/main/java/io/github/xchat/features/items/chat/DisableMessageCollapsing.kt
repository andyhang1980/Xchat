package io.github.xchat.features.items.chat

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import io.github.xchat.utils.reflection.BInt
import io.github.xchat.utils.reflection.bool

@Feature(name = "禁用消息折叠", categories = ["聊天"], description = "阻止聊天消息被折叠")
object DisableMessageCollapsing : SwitchFeature(), IResolveDex {

    private val methodFoldMsg by dexMethod {
        matcher {
            usingStrings(".msgsource.sec_msg_node.clip-len")
            paramTypes(BInt, CharSequence::class.java, null, bool, null, null)
        }
    }

    override fun onEnable() {
        methodFoldMsg.hookBefore {
            result = null
        }
    }
}
