package com.startingground.cognebus.utilities

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.ImageDB
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun deleteImageFileById(imageId: Long, context: Context){
        val imageFile = getImageFile(imageId, context) ?: return
        if(imageFile.exists()){
            imageFile.delete()
        }
    }


    fun getImageFile(imageId: Long, context: Context): File?{
        return try {
            val imageDirectory = context.getExternalFilesDir("images")
            File(imageDirectory, "$imageId.jpg")
        } catch(e: Exception) {
            null
        }
    }


    fun saveBitmapToFile(bitmap: Bitmap, file: File){
        val fileOutputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

        fileOutputStream.flush()
        fileOutputStream.close()
    }


    suspend fun copyImagesTo(imageList: List<ImageDB>, destinationFlashcardId: Long, database: CognebusDatabase, dataViewModel: DataViewModel){
        imageList.forEach {
            val imageCopy = it.copy(imageId = 0L, flashcardId = destinationFlashcardId)
            val imageCopyId = database.imageDatabaseDao.insert(imageCopy)

            val imageFile = dataViewModel.createImageFileOrGetExisting(it.imageId) ?: throw Exception("Could not get original image file!")
            val imageCopyFile = dataViewModel.createImageFileOrGetExisting(imageCopyId) ?: throw Exception("Could not create copy of image file")
            dataViewModel.copyFileFromUri(imageFile.toUri(), imageCopyFile)

            replaceImageIdsInsideFlashcard(it.imageId, imageCopyId, destinationFlashcardId, database)
        }
    }


    private suspend fun replaceImageIdsInsideFlashcard(oldImageId: Long, newImageId: Long, flashcardId: Long, database: CognebusDatabase){
        val flashcard = database.flashcardDatabaseDao.getFlashcardByFlashcardId(flashcardId)
        flashcard.question = flashcard.question.replace("src=\"$oldImageId\"", "src=\"$newImageId\"")
        flashcard.answer = flashcard.answer.replace("src=\"$oldImageId\"", "src=\"$newImageId\"")
        database.flashcardDatabaseDao.update(flashcard)
    }
}