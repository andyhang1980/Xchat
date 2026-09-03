package io.github.xchat.application

import android.app.Application
import io.github.xchat.utils.HostInfo

class ModuleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        HostInfo.init(this)
    }
}
