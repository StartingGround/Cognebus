package com.startingground.cognebus.utilities

import java.util.*

object TimeCognebusUtils {
    fun getBeginningOfNextDayInMilliseconds(): Long{
        return Calendar.getInstance().apply {
            this.add(Calendar.DAY_OF_YEAR, 1)
            this.set(Calendar.SECOND, 1)
            this.set(Calendar.MINUTE, 0)
            this.set(Calendar.HOUR_OF_DAY, 0)
        }.timeInMillis
    }
}