package io.github.xchat.features.api.ui

import android.content.Context
import android.content.DialogInterface
import io.github.xchat.dexkit.abc.IResolveDex
import io.github.xchat.dexkit.dsl.dexClass
import io.github.xchat.features.core.ApiFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.utils.reflection.BString

@Feature(name = "对话框 API", categories = ["API"], description = "提供显示微信自带对话框的能力")
object WeAlertDialogApi : ApiFeature(), IResolveDex {

    private val classMmAlert by dexClass {
        matcher {
            usingEqStrings("MicroMsg.MMAlert")
        }
    }

    fun showAlertDialog(
        context: Context,
        content: String,
        title: String? = null,
        onClickOk: (DialogInterface) -> Unit = {},
        onClickCancel: (DialogInterface) -> Unit = {},
        okText: String = "确定",
        cancelText: String = "取消"
    ) {
        classMmAlert.reflekt()
            .firstMethod {
                parameters(Context::class, BString, BString, BString, BString, DialogInterface.OnClickListener::class, DialogInterface.OnClickListener::class)
            }.invokeStatic(
                context, content, title ?: "", okText, cancelText,
                DialogInterface.OnClickListener { di, _ -> onClickOk(di) },
                DialogInterface.OnClickListener { di, _ -> onClickCancel(di) })
    }
}
