package io.github.xchat.features.api.net.abc

import io.github.xchat.features.api.net.models.SignResult
import org.json.JSONObject

interface ISigner {
    fun match(cgiId: Int): Boolean
    fun sign(cl: ClassLoader, json: JSONObject): SignResult
}
