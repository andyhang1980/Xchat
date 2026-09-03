package io.github.xchat.features.items.scripting_js

import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature

@Feature(name = "触发器：收到消息 (JS)", categories = ["脚本 (JS)"], description = "收到消息时是否执行 onMessage()")
object OnMessage : SwitchFeature()
