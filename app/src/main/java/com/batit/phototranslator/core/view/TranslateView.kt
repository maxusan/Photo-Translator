package com.batit.phototranslator.core.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class TranslateView(context: Context, attributeSet: AttributeSet) :
    AppCompatImageView(context, attributeSet) {

    private var bitmap: Bitmap? = null
    private var bitmapMatrix: Matrix = Matrix()
    private var matrixScaled: Boolean = false

    fun setImage(bitmap: Bitmap){
        this.bitmap = bitmap
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            if(!matrixScaled){
                bitmapMatrix.setRectToRect(
                    RectF(0f, 0f, it.width.toFloat(), it.height.toFloat()),
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    Matrix.ScaleToFit.CENTER
                )
                matrixScaled = true
            }
            canvas.drawBitmap(it, bitmapMatrix, null)
        }

    }

}