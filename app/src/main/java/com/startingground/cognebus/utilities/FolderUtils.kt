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
}