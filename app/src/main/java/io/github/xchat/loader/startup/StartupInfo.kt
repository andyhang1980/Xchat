package io.github.xchat.loader.startup

import io.github.xchat.loader.abc.IHookBridge
import io.github.xchat.loader.abc.ILoaderService

object StartupInfo {

    lateinit var loaderService: ILoaderService
    var hookBridge: IHookBridge? = null
}
