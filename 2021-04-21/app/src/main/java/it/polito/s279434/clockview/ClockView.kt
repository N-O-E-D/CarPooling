package it.polito.s279434.clockview

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
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

        private var startTouch: PointF? = null

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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when(event?.actionMasked){
            MotionEvent.ACTION_DOWN -> {
                startTouch = PointF(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val currentTouch = PointF(event.x, event.y)
                // compute if moving CW or CCW and update mitue handle accordly

                val cx = width*0.5f
                val cy = height*0.5f
                if(startTouch != null) {
                    val dx1 = startTouch!!.x - cx
                    val dy1 = startTouch!!.y - cy
                    val dx2 = currentTouch.x - cx
                    val dy2 = currentTouch.y - cy
                    val cross = dx1 * dy2 - dx2 * dy1
                    if (cross > 0){
                            m = m + 1
                        if (m == 0)
                            h = h + 1
                    }else {
                            m = m-1
                            if (m < 0){
                                m = 60+m
                                h = h-1
                            }
                    }
                    startTouch = currentTouch
                }


            }
            MotionEvent.ACTION_UP -> {
                startTouch = null
            }
            MotionEvent.ACTION_CANCEL -> {
                startTouch = null
            }
        }
        return super.onTouchEvent(event)
    }
}