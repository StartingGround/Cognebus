package com.startingground.cognebus.utilities

import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.Folder
import javax.inject.Inject

class FolderUtils @Inject constructor(
    private val fileDBUtils: FileDBUtils,
    private val database: CognebusDatabase
){

    suspend fun copyFoldersTo(folderList: List<Folder>, destinationFolderId: Long?){
        val foldersInDestination = database.folderDatabaseDao.getFoldersByParentFolderId(destinationFolderId)
        val folderNamesInDestination = foldersInDestination.map { it.name }

        for(folder in folderList){
            val thereIsDestinationFolderWithSameName = folderNamesInDestination.contains(folder.name)

            val newSubDestinationFolderId = if (thereIsDestinationFolderWithSameName) {
                val destinationFolderWithSameName = foldersInDestination.find { it.name == folder.name }!!
                destinationFolderWithSameName.folderId
            } else {
                val folderCopy = folder.copy(folderId = 0L, parentFolderId = destinationFolderId)
                database.folderDatabaseDao.insert(folderCopy)
            }

            val filesInFolder = database.fileDatabaseDao.getFilesByFolderId(folder.folderId)
            if(filesInFolder.isNotEmpty()){
                fileDBUtils.copyFilesTo(filesInFolder, newSubDestinationFolderId)
            }

            val subFolderList = database.folderDatabaseDao.getFoldersByParentFolderId(folder.folderId)
            if(subFolderList.isEmpty()) continue

            copyFoldersTo(subFolderList, newSubDestinationFolderId)
        }
    }


    suspend fun isThereFolderWithSameName(folderList: List<Folder>, destinationFolderId: Long?): Boolean{
        val foldersInDestination = database.folderDatabaseDao.getFoldersByParentFolderId(destinationFolderId)

        val folderListNames = folderList.map { it.name }
        val matchingFolder = foldersInDestination.find { it.name in folderListNames }

        return matchingFolder != null
    }


    suspend fun isThereSameFileWithinFolders(folderList: List<Folder>, destinationFolderId: Long?): Boolean{
        val foldersInDestination = database.folderDatabaseDao.getFoldersByParentFolderId(destinationFolderId)

        val folderListNames = folderList.map { it.name }
        val matchingDestinationFolders = foldersInDestination.filter { it.name in folderListNames }

        matchingDestinationFolders.forEach { folder ->
            val originFolder = folderList.find { it.name == folder.name } ?: return@forEach
            val filesInsideOriginFolder = database.fileDatabaseDao.getFilesByFolderId(originFolder.folderId)

            var thereIsFileWithSameName = fileDBUtils.isThereFileWithSameName(filesInsideOriginFolder, folder.folderId)
            if(thereIsFileWithSameName) return true

            val foldersInsideOriginFolder = database.folderDatabaseDao.getFoldersByParentFolderId(originFolder.folderId)
            thereIsFileWithSameName = isThereSameFileWithinFolders(foldersInsideOriginFolder, folder.folderId)
            if(thereIsFileWithSameName) return true
        }

        return false
    }
}