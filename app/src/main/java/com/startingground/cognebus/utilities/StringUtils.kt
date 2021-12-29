package com.startingground.cognebus.utilities

object StringUtils {
    fun getErrorForInvalidIntegerValueInString(
        text: String,
        invalidInputError: String,
        minimalValue: Int,
        valueUnderError: String
    ): String?{
        if(text.isEmpty() || text == "-"){
            return invalidInputError
        }
        val value = text.toInt()
        if(value < minimalValue){
            return valueUnderError
        }
        return null
    }
}