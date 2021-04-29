package it.polito.s279434.clockview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation

class ClockView(context: Context,
                attrSet: AttributeSet?,
                defStyle: Int)
        : View(context, attrSet, defStyle) {
        constructor(context: Context): this(context, null, 0)
        constructor(context: Context, attrSet: AttributeSet?): this(context, attrSet, 0)

        var h:Int = 0
        set(value){
            field = (value%12)
            invalidate()
        }
        var m:Int = 0
        set(value){
            field = (value%60)
            invalidate()
        }

        private val paint1: Paint
        private val paint2: Paint
        private val path = Path()

        // so now it's indipendet from device
        private val d = context.resources.displayMetrics.density

        init{
            paint1 = Paint()
            paint1.style = Paint.Style.STROKE
            paint1.color = Color.rgb(20, 20, 20)
            //thickness of line
            paint1.strokeWidth = 3 * d

            paint2 = Paint()
            paint2.style = Paint.Style.FILL_AND_STROKE
            paint2.color = Color.rgb(140, 140, 140)
            paint2.strokeWidth = 3*d //1dp in all device
            paint2.setShadowLayer(10*d, 5*d, 5*d, Color.argb(128, 20, 20, 20) )
        }

    override fun onSaveInstanceState(): Parcelable? {
        return Bundle().apply{
            putInt("h", h)
            putInt("m", m)
            putParcelable("super", super.onSaveInstanceState())
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var prevState = state
        if(prevState is Bundle){
            h = prevState.getInt("h")
            m = prevState.getInt("m")
            prevState = prevState.getParcelable("super")
        }
        super.onRestoreInstanceState(prevState)
    }

    // not need to override on Mesure because we inherit it
    // not a viewgroup -> not onReview (?)
    //canvas is the rectangular area (draw line, draw rectangul)
    override fun onDraw(canvas: Canvas) {
        //super.onDraw(canvas)
        //canvas.drawColor(Color.rgb(255, 0, 0))

        val size = Math.min(canvas.width, canvas.height)*.45f

        //in general 0,0 is in top left
        canvas.withTranslation(canvas.width*.5f, canvas.height*.5f){
            canvas.drawCircle(0f, 0f, size, paint1)
            //30 f = 360 / 12 (withRotation)
            for(i in 1..12){
                withRotation(i*30f) {
                    drawLine(0f, -size*.9f, 0f, -size, paint1)
                }
            }

            //12 o'clock
            //drawLine(0f, 0f, 0f, -size, paint2)


            withRotation(m*6f) {
                path.rewind()
                path.moveTo(0f, size*.1f) //sotto il centro
                path.lineTo(size*.075f, 0f)
                //path.lineTo(0f, -size)
                path.lineTo(0f, -size)
                path.lineTo(-size*.075f, 0f)
                path.close()
                drawPath(path, paint2)
                drawPath(path, paint1)
            }

            withRotation((h+(m/60))*30f) {
                path.rewind()
                path.moveTo(0f, size*.1f) //sotto il centro
                path.lineTo(size*.1f, 0f)
                //path.lineTo(0f, -size)
                path.lineTo(0f, -size*.65f)
                path.lineTo(-size*.1f, 0f)
                path.close()
                drawPath(path, paint2)
                drawPath(path, paint1)
            }


        }


    }
}