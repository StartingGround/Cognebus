package com.startingground.cognebus.utilities

import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.sharedviewmodels.DataViewModel

object FileDBUtils {
    suspend fun copyFilesTo(fileList: List<FileDB>, destinationFolderId: Long?, database: CognebusDatabase, dataViewModel: DataViewModel){
        val filesInDestination = database.fileDatabaseDao.getFilesByFolderId(destinationFolderId)

        val fileListNames = fileList.map { it.name }
        val filesToDeleteInDestination = filesInDestination.filter { it.name in fileListNames }

        if(filesToDeleteInDestination.isNotEmpty()){
            database.fileDatabaseDao.deleteList(filesToDeleteInDestination)
        }

        fileList.forEach {
            val fileCopy = it.copy(fileId = 0L, folderId = destinationFolderId)
            val fileCopyId = database.fileDatabaseDao.insert(fileCopy)

            val flashcards = database.flashcardDatabaseDao.getFlashcardsByFileId(it.fileId)
            if(flashcards.isEmpty()) return@forEach

            FlashcardUtils.copyFlashcardsTo(flashcards, fileCopyId, database, dataViewModel)
        }
    }


    suspend fun isThereFileWithSameName(fileList: List<FileDB>, destinationFolderId: Long?, database: CognebusDatabase): Boolean{
        val filesInDestination = database.fileDatabaseDao.getFilesByFolderId(destinationFolderId)

        val fileListNames = fileList.map { it.name }
        val matchingFile = filesInDestination.find { it.name in fileListNames }

        return matchingFile != null
    }
}