package com.kuikly.kuiklychart

import com.kuikly.kuiklychart.base.BasePager
import com.kuikly.kuiklychart.base.bridgeModule
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.utils.urlParams
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button

@Page("router", supportInLocal = true)
internal class RouterPage : BasePager() {

    private var inputText = ""
    private var inputRef: ViewRef<InputView>? = null

    private val routes = listOf(
        DemoRoute("line_chart_demo", "📈 折线图 Demo", 0xFF4F8FFF, 0xFF6C5CE7, 220f),
        DemoRoute("bar_chart_demo", "📊 柱状图 Demo", 0xFF6C5CE7, 0xFFA29BFE, 240f),
        DemoRoute("area_chart_demo", "🌊 面积图 Demo", 0xFF00B894, 0xFF00CEC9, 220f),
        DemoRoute("pie_chart_demo", "🥧 饼图 Demo", 0xFFE17055, 0xFFD63031, 240f),
        DemoRoute("scatter_chart_demo", "✨ 散点图 Demo", 0xFF8E44AD, 0xFFE056FD, 220f),
        DemoRoute("radar_chart_demo", "🕸️ 雷达图 Demo", 0xFF16A085, 0xFF2ECC71, 220f),
        DemoRoute("stock_chart_demo", "📉 股票图 Demo", 0xFFE74C3C, 0xFFF39C12, 240f),
        DemoRoute("realtime_demo", "⚡ 实时数据 Demo", 0xFF20BF6B, 0xFF26DE81, 260f),
    )

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            RouterNavBar {
                attr {
                    title = TITLE
                    backDisable = true
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.COLUMN)
                    showScrollerIndicator(false)
                    paddingBottom(24f)
                }

                View {
                    attr {
                        allCenter()
                        margin(20f)
                    }
                    View {
                        attr {
                            backgroundColor(Color.WHITE)
                            borderRadius(10f)
                            padding(10f)
                        }
                        Image {
                            attr {
                                src(LOGO)
                                size(
                                    pagerData.pageViewWidth * 0.6f,
                                    (pagerData.pageViewWidth * 0.6f) * (1987f / 2894f),
                                )
                            }
                        }
                    }
                }

                View {
                    attr {
                        flexDirectionRow()
                    }
                    View {
                        attr {
                            margin(all = 10f)
                            marginTop(0f)
                            height(40f)
                            flex(1f)
                            borderRadius(5f)
                        }
                        View {
                            attr {
                                absolutePositionAllZero()
                                backgroundLinearGradient(
                                    Direction.TO_LEFT,
                                    ColorStop(Color(0xFF23D3FD), 0f),
                                    ColorStop(Color(0xFFAD37FE), 1f),
                                )
                            }
                            View {
                                attr {
                                    absolutePosition(top = 1f, left = 1f, right = 1f, bottom = 1f)
                                    backgroundColor(Color.WHITE)
                                    borderRadius(5f)
                                }
                            }
                        }
                        Input {
                            ref { ctx.inputRef = it }
                            attr {
                                flex(1f)
                                fontSize(15f)
                                color(Color(0xFFAD37FE))
                                marginLeft(10f)
                                marginRight(10f)
                                placeholder(PLACEHOLDER)
                                autofocus(true)
                                placeholderColor(Color(0xAA23D3FD))
                            }
                            event {
                                textDidChange { ctx.inputText = it.text }
                            }
                        }
                    }
                    Button {
                        attr {
                            size(80f, 40f)
                            borderRadius(20f)
                            marginLeft(2f)
                            marginRight(15f)
                            backgroundLinearGradient(
                                Direction.TO_BOTTOM,
                                ColorStop(Color(0xAA23D3FD), 0f),
                                ColorStop(Color(0xAAAD37FE), 1f),
                            )
                            titleAttr {
                                text(JUMP_TEXT)
                                fontSize(17f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                if (ctx.inputText.isEmpty()) {
                                    ctx.bridgeModule.toast("请输入PageName")
                                } else {
                                    ctx.inputRef?.view?.blur()
                                    getPager().acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
                                        .setItem(CACHE_KEY, ctx.inputText.trim())
                                    ctx.jumpPage(ctx.inputText)
                                }
                            }
                        }
                    }
                }

                Text {
                    attr {
                        fontSize(15f)
                        marginLeft(10f)
                        marginTop(5f)
                        text(
                            if (pagerData.params.optString("execute_mode") == "1") AAR_MODE_TIP else TIP,
                        )
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFFAD37FE), 0f),
                            ColorStop(Color(0xFF23D3FD), 1f),
                        )
                    }
                }

                ctx.routes.forEach { route ->
                    View {
                        attr {
                            allCenter()
                            marginLeft(20f)
                            marginRight(20f)
                            marginBottom(20f)
                        }
                        View {
                            attr {
                                size(route.buttonWidth, 48f)
                                borderRadius(24f)
                                allCenter()
                                backgroundLinearGradient(
                                    Direction.TO_RIGHT,
                                    ColorStop(Color(route.startColor), 0f),
                                    ColorStop(Color(route.endColor), 1f),
                                )
                                boxShadow(BoxShadow(0f, 4f, 12f, Color(route.startColor and 0x33FFFFFF)))
                            }
                            Text {
                                attr {
                                    text(route.label)
                                    fontSize(18f)
                                    fontWeightSemiBold()
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    ctx.jumpPage(route.pageName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        val cacheInputText = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
            .getItem(CACHE_KEY)
        if (cacheInputText.isNotEmpty()) {
            inputText = cacheInputText
            inputRef?.view?.setText(cacheInputText)
        }
    }

    private fun jumpPage(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) {
            bridgeModule.toast("请输入PageName")
            return
        }
        val params = if (input.contains('=')) urlParams(input) else urlParams("pageName=$input")
        val pageData = JSONObject()
        params.forEach { pageData.put(it.key, it.value) }
        var pageName = pageData.optString("pageName")
        if (pageName.isEmpty()) {
            pageName = input.substringBefore('&').trim()
            pageData.put("pageName", pageName)
        }
        if (pageName.isEmpty()) {
            bridgeModule.toast("请输入有效的PageName")
            return
        }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, pageData)
    }

    private data class DemoRoute(
        val pageName: String,
        val label: String,
        val startColor: Long,
        val endColor: Long,
        val buttonWidth: Float,
    )

    private companion object {
        const val PLACEHOLDER = "输入pageName"
        const val TIP = "输入规则：router 或者 router&key=value (&后面为页面参数)"
        const val CACHE_KEY = "router_last_input_key2"
        const val LOGO = "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
        const val JUMP_TEXT = "跳转"
        const val TITLE = "Kuikly页面路由"
        private const val AAR_MODE_TIP = "如：router 或者 router&key=value （&后面为页面参数）"
    }
}
