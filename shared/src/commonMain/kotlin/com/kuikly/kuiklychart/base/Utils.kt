package com.kuikly.kuiklychart.base

import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.manager.PagerManager

internal object Utils : BaseObject() {

    fun bridgeModule(pager: String): BridgeModule {
        return PagerManager.getPager(pager).acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
    }

    fun logToNative(pagerId: String, content: String) {
        // logToNaive
        bridgeModule(pagerId).log(content)
    }

}
