package com.startingground.cognebus

import androidx.core.net.toUri
import androidx.lifecycle.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.database.entity.ImageDB
import kotlinx.coroutines.launch


class ClipboardViewModel(private val database: CognebusDatabase, private val dataViewModel: DataViewModel) : ViewModel(){
    private var selectedFilesDatabase: LiveData<List<FileDB>> = liveData { }
    private var selectedFoldersDatabase: LiveData<List<Folder>> = liveData {  }

    private var _thereIsContentReadyToBePasted: MediatorLiveData<Boolean> = MediatorLiveData()
    val thereIsContentReadyToBePasted: LiveData<Boolean> get() = _thereIsContentReadyToBePasted

    init {
        _thereIsContentReadyToBePasted.value = false
    }


    private fun addObserversForConfirmingAvailabilityOfSelectedItems(){
        _thereIsContentReadyToBePasted.addSource(selectedFilesDatabase, observerForConfirmingAvailabilityOfSelectedItems)
        _thereIsContentReadyToBePasted.addSource(selectedFoldersDatabase, observerForConfirmingAvailabilityOfSelectedItems)
    }


    private fun removeObserversForConfirmingAvailabilityOfSelectedItems(){
        _thereIsContentReadyToBePasted.removeSource(selectedFilesDatabase)
        _thereIsContentReadyToBePasted.removeSource(selectedFoldersDatabase)
        _thereIsContentReadyToBePasted.value = false
    }


    private val observerForConfirmingAvailabilityOfSelectedItems = Observer<Any>{
        if(selectedFilesIds.isEmpty() && selectedFoldersIds.isEmpty()){
            _thereIsContentReadyToBePasted.value = false
            return@Observer
        }

        val selectedFilesDatabaseIds: List<Long> = selectedFilesDatabase.value?.map { it.fileId } ?: listOf()
        val selectedFoldersDatabaseIds: List<Long> = selectedFoldersDatabase.value?.map{ it.folderId } ?: listOf()

        val filesMatch = selectedFilesIds.containsAll(selectedFilesDatabaseIds) && selectedFilesDatabaseIds.containsAll(selectedFilesIds)
        val foldersMatch = selectedFoldersIds.containsAll(selectedFoldersDatabaseIds) && selectedFoldersDatabaseIds.containsAll(selectedFoldersIds)
        if(!filesMatch || !foldersMatch){
            _thereIsContentReadyToBePasted.value = false
            return@Observer
        }

        determineOriginFolder()
        _thereIsContentReadyToBePasted.value = true
    }


    private fun determineOriginFolder(){
        originFolder = when{
            (selectedFilesDatabase.value?.size ?: 0) > 0 -> selectedFilesDatabase.value!!.first().folderId
            (selectedFoldersDatabase.value?.size ?: 0) > 0 -> selectedFoldersDatabase.value!!.first().parentFolderId
            else -> throw Exception("Can't determine origin folder.")
        }
    }


    private var selectedFilesIds: List<Long> = listOf()
    private var selectedFoldersIds: List<Long> = listOf()

    private var cutSelected: Boolean = false
    private var originFolder: Long? = null


    fun copySelectedFilesAndFolders(files: List<FileDB>, folders: List<Folder>, cutEnabled: Boolean = false){
        removeObserversForConfirmingAvailabilityOfSelectedItems()
        cutSelected = cutEnabled

        selectedFilesIds = files.map { it.fileId }
        selectedFoldersIds = folders.map { it.folderId }

        selectedFilesDatabase = database.fileDatabaseDao.getLiveDataFilesInFileIdList(selectedFilesIds)
        selectedFoldersDatabase = database.folderDatabaseDao.getLiveDataFoldersInFolderIdList(selectedFoldersIds)

        addObserversForConfirmingAvailabilityOfSelectedItems()
    }


    private var _errorCopyingToSourceFolder: MutableLiveData<Boolean> = MutableLiveData(false)
    val errorCopyingToSourceFolder: LiveData<Boolean> get() = _errorCopyingToSourceFolder


    fun removeErrorCopyingToSourceFolder(){
        _errorCopyingToSourceFolder.value = false
    }


    fun pasteSelectedToFolder(destinationFolder: Long?){
        if(_thereIsContentReadyToBePasted.value != true) return

        if(originFolder == destinationFolder) return

        viewModelScope.launch {
            if(destinationFolderIsInOneOfSelectedFolders(destinationFolder)) {
                _errorCopyingToSourceFolder.value = true
                return@launch
            }

            selectedFilesDatabase.value?.let {
                if(it.isEmpty()) return@let

                copyFilesTo(it, destinationFolder)

                if(!cutSelected) return@let

                database.fileDatabaseDao.deleteList(it)
            }

            selectedFoldersDatabase.value?.let {
                if(it.isEmpty()) return@let

                copyFoldersTo(it, destinationFolder)

                if(!cutSelected) return@let

                database.folderDatabaseDao.deleteList(it)
            }

            dataViewModel.deleteUnusedImages()

            cutSelected = false
        }
    }


    private suspend fun destinationFolderIsInOneOfSelectedFolders(destinationFolderId: Long?): Boolean{
        var folderId = destinationFolderId

        //Null is root directory
        while(folderId != null){
            if(folderId in selectedFoldersIds) return true

            folderId = database.folderDatabaseDao.getFolderByFolderId(folderId).parentFolderId
        }

        return false
    }


    private suspend fun copyFoldersTo(folderList: List<Folder>, destinationFolderId: Long?){
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
                copyFilesTo(filesInFolder, newSubDestinationFolderId)
            }

            val subFolderList = database.folderDatabaseDao.getFoldersByParentFolderId(folder.folderId)
            if(subFolderList.isEmpty()) continue

            copyFoldersTo(subFolderList, newSubDestinationFolderId)
        }
    }


    private suspend fun copyFilesTo(fileList: List<FileDB>, destinationFolderId: Long?){
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

            copyFlashcardsTo(flashcards, fileCopyId)
        }
    }


    private suspend fun copyFlashcardsTo(flashcardList: List<FlashcardDB>, destinationFileId: Long){
        flashcardList.forEach {
            val flashcardCopy = it.copy(flashcardId = 0L, fileId = destinationFileId)
            val flashcardCopyId = database.flashcardDatabaseDao.insert(flashcardCopy)

            val images = database.imageDatabaseDao.getImagesByFlashcardId(it.flashcardId)
            if(images.isEmpty()) return@forEach

            copyImagesTo(images, flashcardCopyId)
        }
    }


    private suspend fun copyImagesTo(imageList: List<ImageDB>, destinationFlashcardId: Long){
        imageList.forEach {
            val imageCopy = it.copy(imageId = 0L, flashcardId = destinationFlashcardId)
            val imageCopyId = database.imageDatabaseDao.insert(imageCopy)

            val imageFile = dataViewModel.createImageFileOrGetExisting(it.imageId) ?: throw Exception("Could not get original image file!")
            val imageCopyFile = dataViewModel.createImageFileOrGetExisting(imageCopyId) ?: throw Exception("Could not create copy of image file")
            dataViewModel.copyFileFromUri(imageFile.toUri(), imageCopyFile)

            replaceImageIdsInsideFlashcard(it.imageId, imageCopyId, destinationFlashcardId)
        }
    }


    private suspend fun replaceImageIdsInsideFlashcard(oldImageId: Long, newImageId: Long, flashcardId: Long){
        val flashcard = database.flashcardDatabaseDao.getFlashcardByFlashcardId(flashcardId)
        flashcard.question = flashcard.question.replace("src=\"$oldImageId\"", "src=\"$newImageId\"")
        flashcard.answer = flashcard.answer.replace("src=\"$oldImageId\"", "src=\"$newImageId\"")
        database.flashcardDatabaseDao.update(flashcard)
    }
}