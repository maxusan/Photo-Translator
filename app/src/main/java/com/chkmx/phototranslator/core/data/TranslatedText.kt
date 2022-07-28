package com.chkmx.phototranslator.core.data

import android.graphics.Rect
import java.util.*

data class TranslatedText(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val boundingBox: Rect = Rect(),
    var rectScaled: Boolean = false
) {
}