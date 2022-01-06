package com.batit.phototranslator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import com.google.mlkit.vision.text.Text

class CustomView constructor(
    context: Context,
    attributeSet: AttributeSet
) : androidx.appcompat.widget.AppCompatImageView(context, attributeSet) {


    var rw: List<Text.TextBlock>? = null
        set(value) {
            field = value
            invalidate()
        }


    var paint: Paint = Paint()
    var paint1: Paint = Paint()


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        rw?.forEach {
            Log.e("logs", it.text)
            val temp = it.boundingBox
            temp?.left = temp?.left!! + 2
            temp.top = temp.top!! + 2
            temp.right = temp.right!! + 2
            temp.bottom = temp.bottom!! + 2
            paint.color = Color.BLACK
            paint1.color = Color.RED
            paint1.style = Paint.Style.STROKE
            paint.style = Paint.Style.STROKE
            paint1.strokeWidth = 1f
            paint.strokeWidth = 1f

            it.boundingBox?.let { it1 -> canvas?.drawRect(it1, paint) }
            canvas?.drawRect(temp, paint1)
        }

    }
}