package com.startingground.cognebus.customviews

/*
https://github.com/jstarczewski/MathView

MIT License

Copyright (c) 2018 Jan Starczewski

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import com.startingground.cognebus.MAX_CLICK_TIME
import com.startingground.cognebus.R
import kotlin.properties.Delegates


class MathView : WebView {

    private val path: String = "file:///android_asset/"

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("SetJavaScriptEnabled")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        with(settings) {
            loadWithOverviewMode = true
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        if (attrs != null) {
            val math = context.obtainStyledAttributes(attrs, R.styleable.MathView)
            if (math.hasValue(R.styleable.MathView_text))
                this.text = math.getString(R.styleable.MathView_text)
            if (math.hasValue(R.styleable.MathView_textZoom))
                setInitialScale((resources.displayMetrics.densityDpi) / 100 * math.getInt(R.styleable.MathView_textZoom, 100))
            math.recycle()
        }
    }

    var text: String? by Delegates.observable<String?>("") { _, old, new ->
        if (old != new)
            update()
    }

    var textAlign: TextAlign by Delegates.observable(TextAlign.START) { _, old, new ->
        if (old != new)
            update()
    }


    var textColor: String by Delegates.observable("Black") { _, old, new ->
        if (old != new)
            update()
    }

    var backgroundColor: String by Delegates.observable("White") { _, old, new ->
        if (old != new)
            update()
    }

    var textZoom: Int = 100
        set(value) {
            setInitialScale((resources.displayMetrics.densityDpi) / 100 * value)
        }


    private fun update() = loadDataWithBaseURL(path,
        "<html><head><link rel='stylesheet' href='" + path + "jqmath-0.4.3.css'>" +
                "<script src='" + path + "jquery-1.4.3.min.js'></script>" +
                "<script src='" + path + "jqmath-etc-0.4.5.min.js'></script>" +
                "</head><body><script>var s = '$text';" +
                "M.parseMath(s);document.body.style.color = \"$textColor\";" +
                "document.body.style.background = \"$backgroundColor\";" +
                "document.body.style.textAlign = \"${textAlign.toString().lowercase()}\";" +
                "document.body.style.wordBreak = \"break-word\";" +
                "document.write(s);</script></body>",
        "text/html", "UTF-8", null)

    //My modifications ---------------------------------------------------------------------
    private var clickDownTime = 0L
    private var customClickHandler: (() -> Unit)? = null

    fun setCustomClickHandler(handler: (() -> Unit)?){
        customClickHandler = handler
    }

    init {
        setOnTouchListener { view, event ->
            if(event.action == MotionEvent.ACTION_DOWN){
                clickDownTime = event.eventTime
            }

            if(event.action == MotionEvent.ACTION_UP && (event.eventTime - clickDownTime) <= MAX_CLICK_TIME){
                view.performClick()
                customClickHandler?.let { it() }
            }

            true
        }
    }
}

enum class TextAlign {
    CENTER, START, RIGHT, JUSTIFY
}