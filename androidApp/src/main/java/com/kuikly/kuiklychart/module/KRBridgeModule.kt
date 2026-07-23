package com.kuikly.kuiklychart.module

import android.util.Log
import android.widget.Toast
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.kuikly.kuiklychart.KRApplication
import org.json.JSONObject

class KRBridgeModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            CLOSE_PAGE -> closePage()
            LOG -> log(params)
            TOAST -> toast(params)
            else -> callback?.invoke(
                mapOf(
                    "code" to -1,
                    "message" to "方法不存在"
                )
            )
        }
    }

    private fun log(params: String?) {
        if (params == null) {
            return
        }
        val paramJSON = JSONObject(params)
        Log.i("KuiklyRender", paramJSON.optString("content"))
    }

    private fun toast(params: String?) {
        if (params == null) {
            return
        }
        val paramJSON = JSONObject(params)
        Toast.makeText(
            KRApplication.application,
            paramJSON.optString("content"),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun closePage() {
        activity?.finish()
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
        private const val CLOSE_PAGE = "closePage"
        private const val LOG = "log"
        private const val TOAST = "toast"
    }
}
