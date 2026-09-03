package io.github.xchat.features.items.scripting_js

import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature

@Feature(name = "触发器：发起请求 (JS)", categories = ["脚本 (JS)"], description = "发起请求时是否执行 onRequest()")
object OnRequest : SwitchFeature()
