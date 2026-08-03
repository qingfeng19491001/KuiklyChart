package com.kuikly.kuiklychart.base

import com.tencent.kuikly.core.base.PagerScope

/** 无需显式传递 pagerId 即可访问当前页面的桥接模块。 */
internal val PagerScope.bridgeModule: BridgeModule
    get() = getPager().acquireModule(BridgeModule.MODULE_NAME)
