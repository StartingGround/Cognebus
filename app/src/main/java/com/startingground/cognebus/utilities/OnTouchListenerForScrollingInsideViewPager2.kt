package com.startingground.cognebus.utilities

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs

class OnTouchListenerForScrollingInsideViewPager2(private val view: View?, context: Context) : View.OnTouchListener, GestureDetector.SimpleOnGestureListener() {

    private var mDetector: GestureDetectorCompat = GestureDetectorCompat(context, this)
    private var viewIsScrolling: Boolean = false


    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        mDetector.onTouchEvent(event)

        val action = event?.actionMasked
        if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL){
            viewIsScrolling = false
            view?.parent?.requestDisallowInterceptTouchEvent(false)
        }

        return false
    }


    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        val scrollingIsHorizontal: Boolean = abs(distanceX) > abs(distanceY)
        if(viewIsScrolling || scrollingIsHorizontal) return false

        viewIsScrolling = true
        view?.parent?.requestDisallowInterceptTouchEvent(true)

        return false
    }
}