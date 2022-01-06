package com.batit.phototranslator

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Display
import android.view.WindowManager
import com.google.mlkit.vision.text.Text


class CustomView constructor(
    context: Context,
    attributeSet: AttributeSet
) : androidx.appcompat.widget.AppCompatImageView(context, attributeSet) {


    var rw: List<Text.Line>? = null
        set(value) {
            field = value
            invalidate()
        }
    var tr: ArrayList<String> = arrayListOf()
    set(value) {
        field = value
        invalidate()
    }


    var paint: Paint = Paint()
    var paint1: Paint = Paint()


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay
        val ll = layoutParams
        rw?.forEachIndexed { index, line ->
            if(null == line.boundingBox)
                return

            val hC: Float = (display.height / this.height).toFloat()
            val wC: Float = (display.width / this.width).toFloat()
            if(wC > 1){
                ll.width = (this.width * wC).toInt()
                ll.height = (this.height * wC).toInt()
                line.boundingBox?.top = (line.boundingBox!!.top * wC).toInt()
                line.boundingBox?.left = (line.boundingBox!!.left * wC).toInt()
                line.boundingBox?.right = (line.boundingBox!!.right * wC).toInt()
                line.boundingBox?.bottom = (line.boundingBox!!.bottom * wC).toInt()
                layoutParams = ll
            }
            paint.color = Color.BLACK
            paint1.color = Color.WHITE
            paint1.style = Paint.Style.FILL
            paint1.alpha = 200
            paint.style = Paint.Style.STROKE
            paint1.strokeWidth = 1f
            paint.strokeWidth = 1f

            canvas.drawRect(line.boundingBox!!, paint)
            try{
                if(tr.isNotEmpty()){
                    val textPaint = Paint()
                    textPaint.color = Color.BLACK
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textPaint.textSize = (line!!.boundingBox!!.height()) * wC
//                    textPaint.setShadowLayer((line!!.boundingBox!!.width()) * wC, line.cornerPoints!![0].x.toFloat(), line.cornerPoints!![2].y.toFloat(), Color.GREEN)
//                    textPaint.setShadowLayer(1f, 0f, 0f, Color.BLACK)
//                    textPaint.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
//                paint.textSize = 100f
//                    textPaint = Paint();


                    // Important for certain APIs
//                    setLayerType(LAYER_TYPE_SOFTWARE, textPaint);
                    canvas.drawRect(line.boundingBox!!, paint1)
                    canvas.drawText(tr[index], line.boundingBox!!.left.toFloat(), line.boundingBox!!.bottom.toFloat(), textPaint)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }

//            canvas.drawRect(temp, paint1)
        }

    }
}