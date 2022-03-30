package com.batit.phototranslator.core.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import com.batit.phototranslator.core.data.TranslatedText

class TranslateView(context: Context, attributeSet: AttributeSet) :
    AppCompatImageView(context, attributeSet) {

    private var bitmap: Bitmap? = null
    private var bitmapMatrix: Matrix = Matrix()
    private var matrixScaled: Boolean = false

    private var textsList: List<TranslatedText>? = null

    private var transX: Float = 0f
    private var transY: Float = 0f
    private var scX: Float = 0f
    private var scY: Float = 0f

    private val textPaint: Paint = Paint()
    private val rectPaint: Paint = Paint()

    var showText: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        textPaint.color = Color.BLACK
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textAlign = Paint.Align.CENTER
        rectPaint.color = Color.WHITE
        rectPaint.style = Paint.Style.FILL
    }

    fun setTranslatedText(textsList: List<TranslatedText>) {
        this.textsList = textsList
        invalidate()
    }

    fun getImageWithTranslate(): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bmp)
        bitmap?.let {
            canvas.drawBitmap(it, bitmapMatrix, null)
            textsList?.let {
                if (showText) {
                    drawTextOverImage(it, canvas)
                }
            }
        }
        return Bitmap.createBitmap(
            bmp,
            transX.toInt(),
            transY.toInt(),
            (width - transX * 2).toInt(),
            (height - transY * 2).toInt()
        )
    }

    fun setImage(bitmap: Bitmap) {
        this.bitmap = bitmap
        matrixScaled = false
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            if (!matrixScaled) {
                bitmapMatrix.setRectToRect(
                    RectF(0f, 0f, it.width.toFloat(), it.height.toFloat()),
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    Matrix.ScaleToFit.CENTER
                )
                matrixScaled = true
            }
            canvas.drawBitmap(it, bitmapMatrix, null)
            textsList?.let {
                if (showText) {
                    drawTextOverImage(it, canvas)
                }
            }
        }
    }

    private fun initMatrixVariables(matrix: Matrix) {
        val values = FloatArray(9)
        matrix.getValues(values)
        transX = values[Matrix.MTRANS_X]
        transY = values[Matrix.MTRANS_Y]
        scX = values[Matrix.MSCALE_X]
        scY = values[Matrix.MSCALE_Y]
    }

    private fun drawTextOverImage(textsList: List<TranslatedText>, canvas: Canvas) {
        initMatrixVariables(bitmapMatrix)
        textsList.forEach { text ->
            if (!text.rectScaled) {
                text.boundingBox.left = ((text.boundingBox.left * scX) + transX).toInt()
                text.boundingBox.right = ((text.boundingBox.right * scX) + transX).toInt()
                text.boundingBox.top = ((text.boundingBox.top * scY) + transY).toInt()
                text.boundingBox.bottom = ((text.boundingBox.bottom * scY) + transY).toInt()
                text.rectScaled = true
            }

            textPaint.textSize = (text.boundingBox.bottom - text.boundingBox.top).toFloat()
            textPaint.textScaleX = 1f
            var r = Rect()
            r.set(text.boundingBox)
            textPaint.getTextBounds(text.text, 0, text.text.length, r)
            val scx = text.boundingBox.width().toFloat() / r.width().toFloat()
            if (scx in 0.01f..3f)
                textPaint.textScaleX = scx
//            Log.e("logs", "scx ${scx}")
//            Log.e("logs", "text ${text.text}")
//            Log.e("logs", "r.width ${r.width()}")
//            Log.e("logs", "text.boundingBox.width ${text.boundingBox.width()}")
            canvas.drawRect(text.boundingBox, rectPaint)
            canvas.drawText(
                text.text, text.boundingBox.centerX().toFloat(),
                text.boundingBox.bottom.toFloat(), textPaint
            )
        }
    }


}