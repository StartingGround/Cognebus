package com.startingground.cognebus.sharedviewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.database.entity.ImageDB
import com.startingground.cognebus.utilities.ImageUtils
import kotlinx.coroutines.launch

open class DatabaseInputViewModel(app: Application) : AndroidViewModel(app){

    final override fun <T : Application?> getApplication(): T {
        return super.getApplication()
    }

    val database = CognebusDatabase.getInstance(getApplication<Application>().applicationContext)

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
}