package io.github.xchat.features.items.miniapps

import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexConstructor
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import dev.ujhhgtg.reflekt.reflekt
import dev.ujhhgtg.reflekt.utils.isBuiltin
import org.json.JSONObject
import java.lang.reflect.Field

@Feature(name = "移除嵌入广告", categories = ["小程序"], description = "移除小程序嵌入广告")
object RemoveEmbeddedAds : SwitchFeature(), IResolveDex {

    private val ctorNetSceneJSOperateWxData by dexConstructor {
        matcher {
            declaredClass {
                usingEqStrings("MicroMsg.NetSceneJSOperateWxData", "doScene hash=%d, funcid=%d")
            }
        }
    }
    private val methodBaseTransferRequestOnLoad by dexMethod {
        matcher {
            usingEqStrings("MicroMsg.BaseTransferRequest")
            paramTypes("com.tencent.mm.plugin.brandservice.api.TransferResultInfo")
        }
    }

    private lateinit var protoField: Field

    override fun onEnable() {
        ctorNetSceneJSOperateWxData.hookBefore {
            val json = runCatching { JSONObject(args[1] as String) }.getOrElse { return@hookBefore }
            if (json.getString("api_name") == "webapi_getadvert") {
                json.put("data", json.getJSONObject("data").put("ad_unit_id", ""))
                args[1] = json.toString()
            }
        }

        methodBaseTransferRequestOnLoad.hookBefore {
            val transferResultInfo = args[0]
            if (!::protoField.isInitialized) {
                protoField = transferResultInfo.reflekt()
                    .firstField {
                        type { !it.isBuiltin }
                    }.self
            }

            val proto = protoField.get(transferResultInfo)
            proto.reflekt()
                .fields {
                    type = String::class
                }.forEach {
                    val jsonStr = it.get() as? String? ?: return@forEach
                    if (jsonStr.isBlank()) return@forEach
                    val json = runCatching { JSONObject(jsonStr) }.getOrElse { return@forEach }
                    if (!json.has("ad_slot_data")) return@forEach
                    it.set("{}")
                }
        }
    }
}
