package com.startingground.cognebus.utilities

import android.content.Context
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.database.entity.ImageDB
import kotlinx.coroutines.*


open class DatabaseCognebusUtils(
    protected val appContext: Context,
    protected val imageUtils: ImageUtils,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
){

    protected val database = CognebusDatabase.getInstance(appContext)
    protected var scope: CoroutineScope? = null

    fun createScope(){
        if(scope != null) return
        scope = CoroutineScope(Job() + mainDispatcher)
    }

    fun cancelScope(){
        scope?.cancel()
        scope = null
    }

    fun insertFileToDatabase(file: FileDB){
        scope?.launch {
            database.fileDatabaseDao.insert(file)
        }
    }

    fun updateFileInDatabase(file: FileDB){
        scope?.launch {
            database.fileDatabaseDao.update(file)
        }
    }

    fun deleteFileList(files: List<FileDB>){
        scope?.launch {
            database.fileDatabaseDao.deleteList(files)
            deleteUnusedImages()
        }
    }

    fun insertFolderToDatabase(folder: Folder){
        scope?.launch {
            database.folderDatabaseDao.insert(folder)
        }
    }

    fun deleteFolderList(folders: List<Folder>){
        scope?.launch {
            database.folderDatabaseDao.deleteList(folders)
            deleteUnusedImages()
        }
    }

    fun insertFlashcardToDatabaseAndUpdateUsedImagesInDatabaseAndDeleteUnused(flashcard: FlashcardDB, usedImages: List<ImageDB>, unusedImages: List<ImageDB>){
        scope?.launch {
            val flashcardId = database.flashcardDatabaseDao.insert(flashcard)
            usedImages.forEach{ it.flashcardId = flashcardId }
            database.imageDatabaseDao.updateList(usedImages)
            deleteImages(unusedImages)
        }
    }

    fun updateFlashcardInDatabase(flashcard: FlashcardDB){
        scope?.launch {
            database.flashcardDatabaseDao.update(flashcard)
        }
    }

    fun updateFlashcardsInDatabase(flashcards: List<FlashcardDB>){
        scope?.launch {
            database.flashcardDatabaseDao.updateWithList(flashcards)
        }
    }

    fun deleteFlashcardList(flashcards: List<FlashcardDB>){
        scope?.launch {
            database.flashcardDatabaseDao.deleteList(flashcards)
            deleteUnusedImages()
        }
    }

    fun updateImagesInDatabase(images: List<ImageDB>){
        scope?.launch {
            database.imageDatabaseDao.updateList(images)
        }
    }

    fun deleteUnusedImages() {
        scope?.launch {
            val unusedImages = database.imageDatabaseDao.getUnusedImages()

            if(unusedImages.isEmpty()) return@launch

            unusedImages.forEach { imageUtils.deleteImageFileById(it.imageId, it.fileExtension) }

            //SQLite will break if we send it list of 1000 or more elements to its input
            for(startIndex in unusedImages.indices step 999){
                var endIndex = startIndex + 999
                if(endIndex > unusedImages.size) endIndex = unusedImages.size

                val images = unusedImages.subList(startIndex, endIndex)
                database.imageDatabaseDao.deleteList(images)
            }
        }
    }

    fun deleteImages(images: List<ImageDB>){
        scope?.launch {
            images.forEach { imageUtils.deleteImageFileById(it.imageId, it.fileExtension) }
            database.imageDatabaseDao.deleteList(images)
        }
    }
}