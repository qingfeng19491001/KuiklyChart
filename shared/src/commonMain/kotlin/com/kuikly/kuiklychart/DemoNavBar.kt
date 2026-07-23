package com.kuikly.kuiklychart

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.kuikly.kuiklychart.base.BasePager

internal fun ViewContainer<*, *>.DemoNavBar(pager: BasePager, title: String) {
    View {
        attr {
            paddingTop(pager.pagerData.statusBarHeight)
            backgroundColor(Color.WHITE)
        }
        View {
            attr {
                height(44f)
                allCenter()
            }
            Text {
                attr {
                    text(title)
                    fontSize(17f)
                    fontWeightSemiBold()
                    color(Color(0xFF333333))
                }
            }
        }
        View {
            attr {
                positionAbsolute()
                top(pager.pagerData.statusBarHeight + 10f)
                left(12f)
                size(24f, 24f)
                allCenter()
            }
            Text {
                attr {
                    text("←")
                    fontSize(20f)
                    color(Color(0xFF333333))
                }
            }
            event {
                click {
                    pager.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                }
            }
        }
    }
}

internal fun chartDemoShell(pager: BasePager, title: String, content: ViewBuilder): ViewBuilder {
    return {
        View {
            attr {
                flex(1f)
                flexDirection(com.tencent.kuikly.core.layout.FlexDirection.COLUMN)
                backgroundColor(Color(0xFFF5F6FA))
            }
            DemoNavBar(pager, title)
            content()
        }
    }
}

internal fun chartDemoBody(pager: BasePager, title: String, chart: ViewBuilder): ViewBuilder {
    return chartDemoShell(pager, title) {
        View {
            attr {
                height(
                    responsiveChartHeight(
                        pager.pagerData.pageViewWidth,
                        pager.pagerData.pageViewHeight,
                        pager.pagerData.statusBarHeight,
                    ),
                )
                margin(12f)
            }
            chart()
        }
    }
}

internal fun responsiveChartHeight(
    pageWidth: Float,
    pageHeight: Float,
    statusBarHeight: Float,
): Float {
    val widthBasedHeight = pageWidth * 0.9f
    val availableHeight = (pageHeight - statusBarHeight - 56f).coerceAtLeast(0f)
    return minOf(widthBasedHeight, availableHeight)
}
