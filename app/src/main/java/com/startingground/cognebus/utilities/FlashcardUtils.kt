package com.startingground.cognebus.utilities

import android.content.Context

object FlashcardUtils {
    fun prepareStringForPractice(context: Context, inputText: String, enableHTML: Boolean): String{
        val imageRegex = Regex("src=\"\\d+\"")
        val imageIdRegex = Regex("\\d+")

        var text = imageRegex.replace(inputText){
            imageIdRegex.replace(it.value) index@{ imageId ->
                val imageFile = ImageUtils.getImageFile(imageId.value.toLong(), context)
                if(imageFile?.exists() != true){
                    return@index "null"
                }

                "file://" + imageFile.absolutePath
            }
        }

        if(enableHTML){
            text = text.replace(" ", "&nbsp;<wbr>")
            text = filterAllTagsForWhiteSpaces(text)
            return filterSpecialCharacters(text)
        }

        text = text.replace("<", "&lt;")
        text = text.replace(">", "&gt;")
        text = reverseImageTag(text)

        text = text.replace(" ", "&nbsp;<wbr>")
        text = filterImgTagsForWhiteSpaces(text)
        return filterSpecialCharacters(text)
    }


    private fun filterImgTagsForWhiteSpaces(inputText: String): String{
        val imageTagRegex = Regex("<img([^<>]|<wbr>)+>")

        val text = imageTagRegex.replace(inputText){
            it.value.replace("&nbsp;<wbr>", " ")
        }
        return text
    }


    private fun filterAllTagsForWhiteSpaces(inputText: String): String{
        val tagRegex = Regex("<([^<>]|<wbr>)+>")

        val text = tagRegex.replace(inputText){
            it.value.replace("&nbsp;<wbr>", " ")
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
}