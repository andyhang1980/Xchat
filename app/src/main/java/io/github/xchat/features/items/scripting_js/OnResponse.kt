package io.github.xchat.features.items.scripting_js

import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature

@Feature(name = "触发器：收到响应 (JS)", categories = ["脚本 (JS)"], description = "收到响应时是否执行 onResponse()")
object OnResponse : SwitchFeature()
