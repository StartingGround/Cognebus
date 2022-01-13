package com.startingground.cognebus.utilities

import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.sharedviewmodels.DataViewModel

object FolderUtils {
    suspend fun copyFoldersTo(folderList: List<Folder>, destinationFolderId: Long?, database: CognebusDatabase, dataViewModel: DataViewModel){
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
                FileDBUtils.copyFilesTo(filesInFolder, newSubDestinationFolderId, database, dataViewModel)
            }

            val subFolderList = database.folderDatabaseDao.getFoldersByParentFolderId(folder.folderId)
            if(subFolderList.isEmpty()) continue

            copyFoldersTo(subFolderList, newSubDestinationFolderId, database, dataViewModel)
        }
    }


    suspend fun isThereFolderWithSameName(folderList: List<Folder>, destinationFolderId: Long?, database: CognebusDatabase): Boolean{
        val foldersInDestination = database.folderDatabaseDao.getFoldersByParentFolderId(destinationFolderId)

        val folderListNames = folderList.map { it.name }
        val matchingFolder = foldersInDestination.find { it.name in folderListNames }

        return matchingFolder != null
    }


    suspend fun isThereSameFileWithinFolders(folderList: List<Folder>, destinationFolderId: Long?, database: CognebusDatabase): Boolean{
        val foldersInDestination = database.folderDatabaseDao.getFoldersByParentFolderId(destinationFolderId)

        val folderListNames = folderList.map { it.name }
        val matchingDestinationFolders = foldersInDestination.filter { it.name in folderListNames }

        matchingDestinationFolders.forEach { folder ->
            val originFolder = folderList.find { it.name == folder.name } ?: return@forEach
            val filesInsideOriginFolder = database.fileDatabaseDao.getFilesByFolderId(originFolder.folderId)

            var thereIsFileWithSameName = FileDBUtils.isThereFileWithSameName(filesInsideOriginFolder, folder.folderId, database)
            if(thereIsFileWithSameName) return true

            val foldersInsideOriginFolder = database.folderDatabaseDao.getFoldersByParentFolderId(originFolder.folderId)
            thereIsFileWithSameName = isThereSameFileWithinFolders(foldersInsideOriginFolder, folder.folderId, database)
            if(thereIsFileWithSameName) return true
        }

        return false
    }
}