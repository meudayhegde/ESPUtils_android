package com.github.meudayhegde.fab

import android.content.Context
import kotlin.math.roundToInt

internal object Util {
    fun dpToPx(context: Context, dp: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale).roundToInt()
    }
}
