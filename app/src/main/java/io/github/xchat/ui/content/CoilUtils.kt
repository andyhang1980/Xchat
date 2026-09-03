package io.github.xchat.ui.content

import coil3.ImageLoader
import coil3.request.CachePolicy
import io.github.xchat.utils.HostInfo

val GlobalImageLoader by lazy {
    ImageLoader.Builder(HostInfo.application)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}
