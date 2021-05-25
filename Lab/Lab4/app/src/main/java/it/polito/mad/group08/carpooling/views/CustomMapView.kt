package it.polito.mad.group08.carpooling.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import org.osmdroid.views.MapView


class CustomMapView: MapView {
    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    //constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        super.dispatchTouchEvent(event)
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                this.parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP -> {
                this.parent.requestDisallowInterceptTouchEvent(false)
            }
            else -> super.dispatchTouchEvent(event)
        }

        return true
    }
}