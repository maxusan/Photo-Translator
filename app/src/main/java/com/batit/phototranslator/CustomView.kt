package com.batit.phototranslator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
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

    init {
        paint.color = Color.BLACK
        paint1.color = Color.WHITE
        paint1.style = Paint.Style.FILL
        paint1.alpha = 200
        paint.style = Paint.Style.STROKE
        paint1.strokeWidth = 1f
        paint.strokeWidth = 1f
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = wm.defaultDisplay
        val ll = layoutParams
        if (height != 0 && width != 0) {
            val hC: Float = (display.height.toFloat() / height.toFloat())
            val wC: Float = (display.width.toFloat() / width.toFloat())
//            val q =
//            val a =
            rw?.forEachIndexed { index, line ->
                if (null == line.boundingBox)
                    return
//                Paint().apply {
//                    color = Color.RED
//                    strokeWidth = 1f
//                    style = Paint.Style.FILL
//                    canvas.drawRect(line.boundingBox?.left!!.toFloat(), line.boundingBox?.top!!.toFloat(), line.boundingBox?.right!!.toFloat(),  line.boundingBox?.bottom!!.toFloat(), this)
//                    }
                if (wC > 1) {
                    ll.width = (this.width * wC).toInt()
                    ll.height = (this.height * wC).toInt()
                    line.boundingBox?.top = (line.boundingBox!!.top * wC ).toInt()
                    line.boundingBox?.left = (line.boundingBox!!.left * wC).toInt()
                    line.boundingBox?.right = (line.boundingBox!!.right * wC).toInt()
                    line.boundingBox?.bottom = (line.boundingBox!!.bottom * wC).toInt()
                    layoutParams = ll
                }
//                if (wC > 1 && hC == 1f ) {
//                    ll.width = (this.width * wC / 2).toInt()
//                    ll.height = (this.height * wC / 2).toInt()
//                    line.boundingBox?.top = (line.boundingBox!!.top * wC / 2).toInt()
//                    line.boundingBox?.left = (line.boundingBox!!.left * wC / 2).toInt()
//                    line.boundingBox?.right = (line.boundingBox!!.right * wC / 2).toInt()
//                    line.boundingBox?.bottom = (line.boundingBox!!.bottom * wC/ 2).toInt()
//                    layoutParams = ll
//                }

//                if(hC >= 1){
//                    ll.width = (this.width /2).toInt()
//                    ll.height = (this.height /2).toInt()
//                    line.boundingBox?.top = (line.boundingBox!!.top /2).toInt()
//                    line.boundingBox?.left = (line.boundingBox!!.left /2).toInt()
//                    line.boundingBox?.right = (line.boundingBox!!.right /2).toInt()
//                    line.boundingBox?.bottom = (line.boundingBox!!.bottom /2).toInt()
//                    layoutParams = ll
//                }
//                if(hC >= 1){
//                    ll.height = (this.height /hC).toInt()
//                    line.boundingBox?.top = (line.boundingBox!!.top/ hC ).toInt()
//                    line.boundingBox?.left = (line.boundingBox!!.left / hC).toInt()
//                    line.boundingBox?.right = (line.boundingBox!!.right / hC).toInt()
//                    line.boundingBox?.bottom = (line.boundingBox!!.bottom / hC).toInt()
//                    layoutParams = ll
//                }
                Log.e("logs", line.boundingBox.toString())
//                if (hC > 1f) {
//                    ll.width = (this.width /2).toInt()
//                    ll.height = (this.height /2).toInt()
//                    layoutParams = ll
//                }

//            else if(hC == 1f){
//                ll.width = (this.width / 2).toInt()
//                ll.height = (this.height / 2).toInt()
//                line.boundingBox?.top = (line.boundingBox!!.top / 2).toInt()
//                line.boundingBox?.left = (line.boundingBox!!.left / 2).toInt()
//                line.boundingBox?.right = (line.boundingBox!!.right / 2).toInt()
//                line.boundingBox?.bottom = (line.boundingBox!!.bottom / 2).toInt()
//                layoutParams = ll
//            }


                canvas.drawRect(line.boundingBox!!, paint)
                try {
                    if (tr.isNotEmpty()) {
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
                        canvas.drawText(
                            tr[index],
                            line.boundingBox!!.left.toFloat(),
                            line.boundingBox!!.bottom.toFloat(),
                            textPaint
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }


//            canvas.drawRect(temp, paint1)
            }
        }

    }
}