package com.startingground.cognebus.utilities

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.ImageDB
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ImageUtils @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val fileCognebusUtils: FileCognebusUtils
) {
    fun deleteImageFileById(imageId: Long, fileExtension: String){
        val imageFile = getImageFile(imageId, fileExtension) ?: return
        if(imageFile.exists()){
            imageFile.delete()
        }
    }


    fun getImageFile(imageId: Long, fileExtension: String): File?{
        return try {
            val imageDirectory = appContext.getExternalFilesDir("images")
            File(imageDirectory, "$imageId.$fileExtension")
        } catch(e: Exception) {
            null
        }
    }


    suspend fun saveBitmapToFile(bitmap: Bitmap, file: File){
        withContext(Dispatchers.IO) {
            val fileOutputStream = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

            fileOutputStream.flush()
            fileOutputStream.close()
        }
    }


    suspend fun copyImagesTo(imageList: List<ImageDB>, destinationFlashcardId: Long, database: CognebusDatabase){
        imageList.forEach {
            var imageCopy = it.copy(imageId = 0L, flashcardId = destinationFlashcardId)
            val imageCopyId = database.imageDatabaseDao.insert(imageCopy)
            imageCopy = database.imageDatabaseDao.getImageByImageId(imageCopyId)

            val imageFile = fileCognebusUtils.createFileOrGetExisting("images", "${it.imageId}.${it.fileExtension}")
                ?: throw Exception("Could not get original image file!")

            val imageCopyFile = fileCognebusUtils.createFileOrGetExisting("images", "${imageCopy.imageId}.${imageCopy.fileExtension}")
                ?: throw Exception("Could not create copy of image file")

            fileCognebusUtils.copyFileFromUri(imageFile.toUri(), imageCopyFile)

            replaceImageIdsInsideFlashcard(it.imageId, imageCopy.imageId, destinationFlashcardId, database)
        }
    }


    private suspend fun replaceImageIdsInsideFlashcard(oldImageId: Long, newImageId: Long, flashcardId: Long, database: CognebusDatabase){
        val flashcard = database.flashcardDatabaseDao.getFlashcardByFlashcardId(flashcardId)
        flashcard.question = flashcard.question.replace("src=\"$oldImageId\"", "src=\"$newImageId\"")
        flashcard.answer = flashcard.answer.replace("src=\"$oldImageId\"", "src=\"$newImageId\"")
        database.flashcardDatabaseDao.update(flashcard)
    }
}