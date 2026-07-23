package com.kuikly.kuiklychart.base

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

internal class BridgeModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun closePage() {
        callNativeMethod(CLOSE_PAGE, null, null)
    }

    fun log(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod(LOG, methodArgs, null)
    }

    fun toast(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod(TOAST, methodArgs, null)
    }

    private fun callNativeMethod(methodName: String, data: JSONObject?, callbackFn: com.tencent.kuikly.core.module.CallbackFn?) {
        toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            false
        )
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
        const val CLOSE_PAGE = "closePage"
        const val LOG = "log"
        const val TOAST = "toast"
    }
}
