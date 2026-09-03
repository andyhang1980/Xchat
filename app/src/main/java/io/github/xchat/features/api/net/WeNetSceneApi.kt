package io.github.xchat.features.api.net

import dev.ujhhgtg.reflekt.reflekt
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexClass
import io.github.xchat.dexkit.dsl.dexMethod
import io.github.xchat.features.core.ApiFeature
import io.github.xchat.features.core.Feature

@Feature(name = "NetScene 服务", categories = ["API"], description = "提供 NetScene 发送能力")
object WeNetSceneApi : ApiFeature(), IResolveDex {

    fun sendNetScene(netScene: Any) {
        val queue = classMmKernel.clazz.reflekt()
            .firstMethod {
                returnType = methodAddNetSceneToQueue.method.declaringClass
            }.invokeStatic()!!
        methodAddNetSceneToQueue.method.invoke(queue, netScene, 0)
    }

    internal val classMmKernel by dexClass {
        matcher {
            usingEqStrings("MicroMsg.MMKernel", "Kernel not null, has initialized.")
        }
    }

    internal val methodAddNetSceneToQueue by dexMethod {
        matcher {
            usingEqStrings("MicroMsg.NetSceneQueue", "forbid in waiting: type=", "forbid in running: type=")
        }
    }
}
