package com.kuikly.kuiklychart.base

import com.tencent.kuikly.core.base.PagerScope

/**
 * 老的方式:，需要显式传递 pagerId
 * ```kotlin
 * Utils.bridgeModule(pagerId).toast("message")
 * ```
 *
 * 新方式：无需显式传递 pagerId
 * ```kotlin
 * bridgeModule.toast("message")
 * ```
 */
internal val PagerScope.bridgeModule: BridgeModule
    get() = getPager().acquireModule(BridgeModule.MODULE_NAME)
