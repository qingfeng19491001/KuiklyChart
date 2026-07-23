package com.kuikly.kuiklychart.adapter

import android.graphics.Color
import com.tencent.kuikly.core.render.android.adapter.IKRColorParserAdapter

class KRColorParserAdapter : IKRColorParserAdapter {

    override fun toColor(colorStr: String): Int? {
        colorStr.toLongOrNull()?.toInt()?.let { return it }
        if (colorStr.startsWith("#")) {
            return Color.parseColor(colorStr)
        }
        return null
    }
}
