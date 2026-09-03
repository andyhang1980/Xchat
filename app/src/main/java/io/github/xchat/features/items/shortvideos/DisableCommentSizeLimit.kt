package io.github.xchat.features.items.shortvideos

import dev.ujhhgtg.reflekt.utils.toClass
import io.github.xchat.features.core.Feature
import io.github.xchat.features.core.SwitchFeature
import dev.ujhhgtg.reflekt.reflekt

@Feature(name = "禁用评论长度限制", categories = ["视频号"], description = "禁用视频号发送评论的字数行数限制 (不保证有效, 云端可能有二次限制)")
object DisableCommentSizeLimit : SwitchFeature() {

    override fun onEnable() {
        "com.tencent.mm.plugin.finder.view.FinderCommentFooter".toClass()
            .reflekt().apply {
                firstMethod { name = "getCommentTextLimit" }
                    .hookBefore {
                        result = 9999
                    }

                runCatching {
                    firstMethod { name = "getCommentTextLimitStart" }
                        .hookBefore {
                            result = 9999
                        }
                }

                firstMethod { name = "getCommentTextLineLimit" }
                    .hookBefore {
                        result = 9999
                    }
            }
    }
}
