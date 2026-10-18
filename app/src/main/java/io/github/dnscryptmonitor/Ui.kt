package io.github.dnscryptmonitor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    val surface = Color.rgb(17, 24, 21)
    val raised = Color.rgb(24, 33, 29)
    val raisedAlt = Color.rgb(29, 40, 35)
    val ink = Color.rgb(243, 246, 244)
    val muted = Color.rgb(156, 172, 159)
    val accent = Color.rgb(101, 212, 154)
    val accentInk = Color.rgb(9, 42, 26)
    val danger = Color.rgb(255, 138, 128)

    fun card(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))
        rounded(raised)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { setMargins(0, 0, 0, context.dp(12)) }
    }

    fun button(context: Context, label: String, primary: Boolean = false): Button =
        Button(context).apply {
            text = label
            isAllCaps = false
            textStyle(14f, if (primary) accentInk else ink, true)
            rounded(if (primary) accent else raisedAlt, 14)
            minHeight = context.dp(48)
            stateListAnimator = null
    }
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun TextView.textStyle(size: Float, color: Int = Ui.ink, bold: Boolean = false) {
    textSize = size
    setTextColor(color)
    if (bold) setTypeface(typeface, Typeface.BOLD)
}

fun View.rounded(color: Int, radius: Int = 18, strokeColor: Int? = null) {
    background = GradientDrawable().apply {
        setColor(color)
        cornerRadius = context.dp(radius).toFloat()
        strokeColor?.let { setStroke(context.dp(1), it) }
    }
}
