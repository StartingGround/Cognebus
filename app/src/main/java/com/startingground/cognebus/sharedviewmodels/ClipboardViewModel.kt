package com.startingground.cognebus.sharedviewmodels

import androidx.lifecycle.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.utilities.FileDBUtils
import com.startingground.cognebus.utilities.FlashcardUtils
import com.startingground.cognebus.utilities.FolderUtils
import kotlinx.coroutines.launch


enum class SelectedType{ DIRECTORIES, FLASHCARDS }

class ClipboardViewModel(private val database: CognebusDatabase, private val dataViewModel: DataViewModel) : ViewModel(){

    //Files and folders part ----------------------------------------------------------------------------------------------------

    private var selectedFilesDatabase: LiveData<List<FileDB>> = liveData { }
    private var selectedFoldersDatabase: LiveData<List<Folder>> = liveData {  }

    private val _thereIsContentReadyToBePastedInDirectories: MediatorLiveData<Boolean> = MediatorLiveData()
    val thereIsContentReadyToBePastedInDirectories: LiveData<Boolean> get() = _thereIsContentReadyToBePastedInDirectories

    init {
        _thereIsContentReadyToBePastedInDirectories.value = false
    }


    private fun addObserversForConfirmingAvailabilityOfSelectedItemsInDirectories(){
        _thereIsContentReadyToBePastedInDirectories.addSource(selectedFilesDatabase, observerForConfirmingAvailabilityOfSelectedItemsInDirectories)
        _thereIsContentReadyToBePastedInDirectories.addSource(selectedFoldersDatabase, observerForConfirmingAvailabilityOfSelectedItemsInDirectories)
    }


    private fun removeObserversForConfirmingAvailabilityOfSelectedItemsInDirectories(){
        _thereIsContentReadyToBePastedInDirectories.removeSource(selectedFilesDatabase)
        _thereIsContentReadyToBePastedInDirectories.removeSource(selectedFoldersDatabase)
        _thereIsContentReadyToBePastedInDirectories.value = false
    }


    private var selectedCopyType: SelectedType = SelectedType.DIRECTORIES


    private val observerForConfirmingAvailabilityOfSelectedItemsInDirectories = Observer<Any>{
        if((selectedFilesIds.isEmpty() && selectedFoldersIds.isEmpty()) || selectedCopyType != SelectedType.DIRECTORIES){
            _thereIsContentReadyToBePastedInDirectories.value = false
            return@Observer
        }

        val selectedFilesDatabaseIds: List<Long> = selectedFilesDatabase.value?.map { it.fileId } ?: listOf()
        val selectedFoldersDatabaseIds: List<Long> = selectedFoldersDatabase.value?.map{ it.folderId } ?: listOf()

        val filesMatch = selectedFilesIds.containsAll(selectedFilesDatabaseIds) && selectedFilesDatabaseIds.containsAll(selectedFilesIds)
        val foldersMatch = selectedFoldersIds.containsAll(selectedFoldersDatabaseIds) && selectedFoldersDatabaseIds.containsAll(selectedFoldersIds)
        if(!filesMatch || !foldersMatch){
            _thereIsContentReadyToBePastedInDirectories.value = false
            return@Observer
        }

        determineOriginFolder()
        _thereIsContentReadyToBePastedInDirectories.value = true
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
        removeObserversForConfirmingAvailabilityOfSelectedItemsInDirectories()
        removeObserverForConfirmingAvailabilityOfSelectedFlashcards()
        cutSelected = cutEnabled

        selectedFilesIds = files.map { it.fileId }
        selectedFoldersIds = folders.map { it.folderId }

        selectedFilesDatabase = database.fileDatabaseDao.getLiveDataFilesInFileIdList(selectedFilesIds)
        selectedFoldersDatabase = database.folderDatabaseDao.getLiveDataFoldersInFolderIdList(selectedFoldersIds)

        selectedCopyType = SelectedType.DIRECTORIES
        addObserversForConfirmingAvailabilityOfSelectedItemsInDirectories()
    }


    private var _errorCopyingToSourceFolder: MutableLiveData<Boolean> = MutableLiveData(false)
    val errorCopyingToSourceFolder: LiveData<Boolean> get() = _errorCopyingToSourceFolder


    fun removeErrorCopyingToSourceFolder(){
        _errorCopyingToSourceFolder.value = false
    }


    fun pasteSelectedToFolder(destinationFolder: Long?){
        if(_thereIsContentReadyToBePastedInDirectories.value != true) return

        if(originFolder == destinationFolder) return

        viewModelScope.launch {
            if(destinationFolderIsInOneOfSelectedFolders(destinationFolder)) {
                _errorCopyingToSourceFolder.value = true
                return@launch
            }

            selectedFilesDatabase.value?.let {
                if(it.isEmpty()) return@let

                FileDBUtils.copyFilesTo(it, destinationFolder, database, dataViewModel)

                if(!cutSelected) return@let

                database.fileDatabaseDao.deleteList(it)
            }

            selectedFoldersDatabase.value?.let {
                if(it.isEmpty()) return@let

                FolderUtils.copyFoldersTo(it, destinationFolder, database, dataViewModel)

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


    //Flashcards part -------------------------------------------------------------------------------------------------------

    private var selectedFlashcardsDatabase: LiveData<List<FlashcardDB>> = liveData{ }

    private val _thereAreFlashcardsToBePasted: MediatorLiveData<Boolean> = MediatorLiveData()
    val thereAreFlashcardsToBePasted: LiveData<Boolean> get() = _thereAreFlashcardsToBePasted

    init {
        _thereAreFlashcardsToBePasted.value = false
    }


    private fun addObserverForConfirmingAvailabilityOfSelectedFlashcards(){
        _thereAreFlashcardsToBePasted.addSource(selectedFlashcardsDatabase, observerForConfirmingAvailabilityOfSelectedFlashcards)
    }


    private fun removeObserverForConfirmingAvailabilityOfSelectedFlashcards(){
        _thereAreFlashcardsToBePasted.removeSource(selectedFlashcardsDatabase)
        _thereAreFlashcardsToBePasted.value = false
    }


    private val observerForConfirmingAvailabilityOfSelectedFlashcards = Observer<List<FlashcardDB>> {
        if(selectedFlashcardsIds.isEmpty() || selectedCopyType != SelectedType.FLASHCARDS){
            _thereAreFlashcardsToBePasted.value = false
            return@Observer
        }

        val selectedFlashcardsDatabaseIds: List<Long> = selectedFlashcardsDatabase.value?.map{ it.flashcardId } ?: listOf()

        val flashcardsMatch = selectedFlashcardsDatabaseIds.containsAll(selectedFlashcardsIds)
                && selectedFlashcardsIds.containsAll(selectedFlashcardsDatabaseIds)
        if(!flashcardsMatch){
            _thereAreFlashcardsToBePasted.value = false
            return@Observer
        }

        _thereAreFlashcardsToBePasted.value = true
    }


    private var selectedFlashcardsIds: List<Long> = listOf()


    fun copySelectedFlashcards(flashcards: List<FlashcardDB>, cutEnabled: Boolean = false){
        removeObserverForConfirmingAvailabilityOfSelectedFlashcards()
        removeObserversForConfirmingAvailabilityOfSelectedItemsInDirectories()
        cutSelected = cutEnabled

        selectedFlashcardsIds = flashcards.map { it.flashcardId }

        selectedFlashcardsDatabase = database.flashcardDatabaseDao.getLiveDataFlashcardsInFlashcardIdList(selectedFlashcardsIds)

        selectedCopyType = SelectedType.FLASHCARDS
        addObserverForConfirmingAvailabilityOfSelectedFlashcards()
    }


    fun pasteSelectedFlashcardsToFile(destinationFileId: Long){
        if(_thereAreFlashcardsToBePasted.value != true) return

        val originFileId: Long = selectedFlashcardsDatabase.value!!.first().fileId
        if(destinationFileId == originFileId) return

        viewModelScope.launch {
            selectedFlashcardsDatabase.value?.let {
                if(it.isEmpty()) return@let

                FlashcardUtils.copyFlashcardsTo(it, destinationFileId, database, dataViewModel)

                if(!cutSelected) return@let

                database.flashcardDatabaseDao.deleteList(it)
            }

            dataViewModel.deleteUnusedImages()
            cutSelected = false
        }
    }
}