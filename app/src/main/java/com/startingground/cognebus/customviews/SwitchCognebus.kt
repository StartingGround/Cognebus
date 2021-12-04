package com.startingground.cognebus.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.core.text.HtmlCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.startingground.cognebus.R

class SwitchCognebus(context: Context, attrs: AttributeSet) : SwitchMaterial(context, attrs){

    private var titleText: String = ""
    private var summaryText: String = ""

    init {
        context.withStyledAttributes(attrs, R.styleable.SwitchCognebus){
            titleText = getString(R.styleable.SwitchCognebus_titleText) ?: ""
            summaryText = getString(R.styleable.SwitchCognebus_summaryText) ?: ""
        }
        updateText()
    }

    private fun updateText(){
        text = HtmlCompat.fromHtml("<b>$titleText</b>" + "<br/>" + summaryText , HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}