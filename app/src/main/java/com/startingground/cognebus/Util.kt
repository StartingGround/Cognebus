package com.startingground.cognebus

import android.content.Context
import java.io.File
import java.util.*

const val SORTING_IN_ORDER = 1L
const val SORTING_REVERSE_ORDER = 2L
const val SORTING_SHUFFLED = 3L

const val MAX_CLICK_TIME = 400

const val MINIMAL_CYCLE_INCREMENT = 1
const val MINIMAL_MAX_DAYS_PER_CYCLE = 31

fun getBeginningOfNextDayInMilliseconds(): Long{
    return Calendar.getInstance().apply {
        this.add(Calendar.DAY_OF_YEAR, 1)
        this.set(Calendar.SECOND, 1)
        this.set(Calendar.MINUTE, 0)
        this.set(Calendar.HOUR_OF_DAY, 0)
    }.timeInMillis
}

fun getImageFile(imageId: Long, context: Context): File?{
    return try {
        val imageDirectory = context.getExternalFilesDir("images")
        File(imageDirectory, "$imageId.jpg")
    } catch(e: Exception) {
        null
    }
}

fun deleteImageFileById(imageId: Long, context: Context){
    val imageFile = getImageFile(imageId, context) ?: return
    if(imageFile.exists()){
        imageFile.delete()
    }
}

fun prepareStringForPractice(context: Context, inputText: String, enableHTML: Boolean): String{
    val imageRegex = Regex("src=\"\\d+\"")
    val imageIdRegex = Regex("\\d+")

    var text = imageRegex.replace(inputText){
        imageIdRegex.replace(it.value) index@{ imageId ->
            val imageFile = getImageFile(imageId.value.toLong(), context)
            if(imageFile?.exists() != true){
                return@index "null"
            }

            "file://" + imageFile.absolutePath
        }
    }

    text = text.replace(" ", "&nbsp;")

    if(enableHTML){
        text = filterAllTagsForWhiteSpaces(text)
        return filterSpecialCharacters(text)
    }

    text = text.replace("<", "&lt;")
    text = text.replace(">", "&gt;")
    text = reverseImageTag(text)
    text = filterImgTagsForWhiteSpaces(text)

    return filterSpecialCharacters(text)
}

private fun filterImgTagsForWhiteSpaces(inputText: String): String{
    val imageTagRegex = Regex("<img[^<>]+>")

    val text = imageTagRegex.replace(inputText){
        it.value.replace("&nbsp;", " ")
    }
    return text
}

private fun filterAllTagsForWhiteSpaces(inputText: String): String{
    val tagRegex = Regex("<[^<>]+>")

    val text = tagRegex.replace(inputText){
        it.value.replace("&nbsp;", " ")
    }
    return text
}

private fun filterSpecialCharacters(inputText: String): String{
    var text = inputText.replace("\\","\\\\")
    text = text.replace("\'", "\\\'")
    text = text.replace("\n", "<br>")

    return text
}

private fun reverseImageTag(inputText: String, startIndex: Int = 0): String{
    var text = inputText
    val lessThanIndex = text.indexOf("&lt;img", startIndex)
    if(lessThanIndex > -1){
        val greaterThanIndex = text.indexOf("&gt;", lessThanIndex)
        text = text.replaceRange(greaterThanIndex, greaterThanIndex + 4, ">")
        text = text.replaceRange(lessThanIndex, lessThanIndex + 4, "<")
        text = reverseImageTag(text, lessThanIndex)
    }
    return text
}

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

