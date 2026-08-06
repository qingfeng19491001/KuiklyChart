package com.kuikly.kuiklychart

import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

internal fun ViewContainer<*, *>.demoVariantSection(
    index: Int,
    title: String,
    chartHeight: Float = 236f,
    chart: ViewBuilder,
) = demoVariantSectionContent(index, title, null, chartHeight, chart)

internal fun ViewContainer<*, *>.stockDemoVariantSection(
    index: Int,
    title: String,
    subtitle: String,
    chartHeight: Float = 236f,
    chart: ViewBuilder,
) = demoVariantSectionContent(index, title, subtitle, chartHeight, chart)

private fun ViewContainer<*, *>.demoVariantSectionContent(
    index: Int,
    title: String,
    subtitle: String?,
    chartHeight: Float,
    chart: ViewBuilder,
) {
    View {
        attr {
            marginTop(16f)
            marginLeft(16f)
            marginRight(16f)
            flexDirectionRow()
        }
        View {
            attr {
                size(24f, 24f)
                borderRadius(12f)
                allCenter()
                backgroundColor(Color(0xFFE6F4FF))
                marginRight(8f)
            }
            Text {
                attr {
                    text(index.toString())
                    fontSize(12f)
                    fontWeightSemiBold()
                    color(Color(0xFF1677FF))
                }
            }
        }
        View {
            attr { flex(1f) }
            Text {
                attr {
                    text(title)
                    fontSize(15f)
                    fontWeightSemiBold()
                    color(Color(0xFF262626))
                }
            }
            vif({ subtitle != null }) {
                Text {
                    attr {
                        text(subtitle.orEmpty())
                        fontSize(12f)
                        color(Color(0xFF8C8C8C))
                        marginTop(2f)
                    }
                }
            }
        }
    }
    View {
        attr {
            height(chartHeight)
            marginTop(9f)
            marginLeft(16f)
            marginRight(16f)
            padding(12f)
            borderRadius(12f)
            backgroundColor(Color.WHITE)
            boxShadow(BoxShadow(0f, 4f, 14f, Color(0x12000000)))
        }
        chart()
    }
}
