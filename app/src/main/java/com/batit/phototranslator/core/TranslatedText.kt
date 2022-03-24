package com.batit.phototranslator.core

import android.graphics.Rect
import android.graphics.RectF
import java.util.*

data class TranslatedText(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val boundingBox: Rect = Rect(),
    var rectScaled: Boolean = false
) {
}