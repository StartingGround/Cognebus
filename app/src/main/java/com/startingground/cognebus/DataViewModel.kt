package com.startingground.cognebus

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.database.entity.ImageDB
import com.startingground.cognebus.utilities.FlashcardUtils
import com.startingground.cognebus.utilities.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DataViewModel(app: Application) : AndroidViewModel(app){

    companion object{
        const val SHOW_DOLLAR_SIGN_ALERT = "dollar_sign_alert"
    }

    val database = CognebusDatabase.getInstance(getApplication<Application>())

    fun insertFileToDatabase(file: FileDB){
        viewModelScope.launch {
            database.fileDatabaseDao.insert(file)
        }
    }

    fun updateFileInDatabase(file: FileDB){
        viewModelScope.launch {
            database.fileDatabaseDao.update(file)
        }
    }

    fun deleteFileList(files: List<FileDB>){
        viewModelScope.launch {
            database.fileDatabaseDao.deleteList(files)
            deleteUnusedImages()
        }
    }

    fun insertFolderToDatabase(folder: Folder){
        viewModelScope.launch {
            database.folderDatabaseDao.insert(folder)
        }
    }

    fun deleteFolderList(folders: List<Folder>){
        viewModelScope.launch {
            database.folderDatabaseDao.deleteList(folders)
            deleteUnusedImages()
        }
    }

    fun insertFlashcardToDatabaseAndUpdateUsedImagesInDatabaseAndDeleteUnused(flashcard: FlashcardDB, usedImages: List<ImageDB>, unusedImages: List<ImageDB>){
        viewModelScope.launch {
            val flashcardId = database.flashcardDatabaseDao.insert(flashcard)
            usedImages.forEach{ it.flashcardId = flashcardId }
            database.imageDatabaseDao.updateList(usedImages)
            deleteImages(unusedImages)
        }
    }

    fun updateFlashcardInDatabase(flashcard: FlashcardDB){
        viewModelScope.launch {
            database.flashcardDatabaseDao.update(flashcard)
        }
    }

    fun updateFlashcardsInDatabase(flashcards: List<FlashcardDB>){
        viewModelScope.launch {
            database.flashcardDatabaseDao.updateWithList(flashcards)
        }
    }

    fun deleteFlashcardList(flashcards: List<FlashcardDB>){
        viewModelScope.launch {
            database.flashcardDatabaseDao.deleteList(flashcards)
            deleteUnusedImages()
        }
    }

    fun updateImagesInDatabase(images: List<ImageDB>){
        viewModelScope.launch {
            database.imageDatabaseDao.updateList(images)
        }
    }

    fun deleteUnusedImages() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val unusedImages = database.imageDatabaseDao.getUnusedImages()

            unusedImages.forEach { ImageUtils.deleteImageFileById(it.imageId, context) }

            database.imageDatabaseDao.deleteList(unusedImages)
        }
    }

    fun deleteImages(images: List<ImageDB>){
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            images.forEach { ImageUtils.deleteImageFileById(it.imageId, context) }

            database.imageDatabaseDao.deleteList(images)
        }
    }

    fun copyFileFromUri(uri: Uri, destinationFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val uriFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
            val uriFileChannel = FileInputStream(uriFileDescriptor).channel

            val destinationFileChannel = FileOutputStream(destinationFile).channel

            destinationFileChannel.transferFrom(uriFileChannel, 0, uriFileChannel.size())

            uriFileChannel.close()
            destinationFileChannel.close()
        }
    }

    fun createImageFileOrGetExisting(imageId: Long): File?{
        return try {
            val context = getApplication<Application>().applicationContext
            val imageDirectory = context.getExternalFilesDir("images")
            val file = File(imageDirectory, "$imageId.jpg")
            if(!file.exists()){
                file.createNewFile()
            }
            file
        } catch(e: Exception) {
            null
        }
    }

    fun getStringFromResources(stringResource: Int): String{
        val context = getApplication<Application>().applicationContext
        return context.getString(stringResource)
    }

    fun prepareStringForPracticeCaller(inputText: String, enableHTML: Boolean): String{
        val context = getApplication<Application>().applicationContext
        return FlashcardUtils.prepareStringForPractice(context, inputText, enableHTML)
    }


    private val preferences = PreferenceManager.getDefaultSharedPreferences(app.applicationContext)


    fun getShowDollarSignAlert(): Boolean{
        return preferences.getBoolean(SHOW_DOLLAR_SIGN_ALERT, true
        )
    }


    fun setShowDollarSignAlert(state: Boolean){
        with(preferences.edit()){
            putBoolean(SHOW_DOLLAR_SIGN_ALERT, state)
            apply()
        }
    }

    fun saveImageBitmapToUri(imageBitmap: Bitmap, imageId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            val imageFile = createImageFileOrGetExisting(imageId)
            val fileOutputStream = FileOutputStream(imageFile)

            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

            fileOutputStream.flush()
            fileOutputStream.close()
        }
    }
}