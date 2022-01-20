package com.startingground.cognebus.utilities

import android.content.Context
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.ImageDB
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FlashcardUtils @Inject constructor(
    @ApplicationContext private val context: Context
){

    fun prepareStringForPractice(inputText: String, enableHTML: Boolean, imageList: List<ImageDB>): String{
        val imageRegex = Regex("src=\"\\d+\"")
        val imageIdRegex = Regex("\\d+")

        var text = imageRegex.replace(inputText){

            imageIdRegex.replace(it.value) index@{ imageId ->
                val imageIdNumber = imageId.value.toLong()
                val imageDB = imageList.find {img -> imageIdNumber == img.imageId } ?: return@index imageId.value

                val imageFile = ImageUtils.getImageFile(imageDB.imageId, imageDB.fileExtension, context) ?: return@index imageId.value

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


    suspend fun copyFlashcardsTo(flashcardList: List<FlashcardDB>, destinationFileId: Long, database: CognebusDatabase, dataViewModel: DataViewModel){
        flashcardList.forEach {
            val flashcardCopy = it.copy(flashcardId = 0L, fileId = destinationFileId)
            val flashcardCopyId = database.flashcardDatabaseDao.insert(flashcardCopy)

            val images = database.imageDatabaseDao.getImagesByFlashcardId(it.flashcardId)
            if(images.isEmpty()) return@forEach

            ImageUtils.copyImagesTo(images, flashcardCopyId, database, dataViewModel)
        }
    }
}