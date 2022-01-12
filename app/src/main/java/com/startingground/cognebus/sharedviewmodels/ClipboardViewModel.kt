package com.startingground.cognebus.sharedviewmodels

import android.view.View
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


    private val _pasteInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val pasteInProgress: LiveData<Boolean> get() = _pasteInProgress

    val pasteProgressIndicatorVisibility: LiveData<Int> = Transformations.map(_pasteInProgress){
        if(it) View.VISIBLE else View.GONE
    }

    private val _pasteProgressPercentage: MutableLiveData<Int> = MutableLiveData(0)
    val pasteProgressPercentage: LiveData<Int> get() = _pasteProgressPercentage


    fun pasteSelectedToFolder(destinationFolder: Long?){
        if(_thereIsContentReadyToBePastedInDirectories.value != true) return

        if(originFolder == destinationFolder) return

        if(_pasteInProgress.value == true) return

        _pasteProgressPercentage.value = 0
        _pasteInProgress.value = true

        viewModelScope.launch {
            if(destinationFolderIsInOneOfSelectedFolders(destinationFolder)) {
                _errorCopyingToSourceFolder.value = true
                _pasteInProgress.value = false
                return@launch
            }

            var totalNumberOfItems: Int = selectedFilesDatabase.value?.size ?: 0
            totalNumberOfItems += selectedFoldersDatabase.value?.size ?: 0

            //When percentage is 0 progress indicator is not visible
            //this will give indicator one more percentage increment that is visible at start
            totalNumberOfItems++
            var numberOfPastedItems = 1
            _pasteProgressPercentage.value = numberOfPastedItems * 100 / totalNumberOfItems


            selectedFilesDatabase.value?.let {
                if(it.isEmpty()) return@let

                it.forEach { file ->
                    FileDBUtils.copyFilesTo(listOf(file), destinationFolder, database, dataViewModel)

                    numberOfPastedItems++
                    _pasteProgressPercentage.value = numberOfPastedItems * 100 / totalNumberOfItems
                }

                if(!cutSelected) return@let

                database.fileDatabaseDao.deleteList(it)
            }

            selectedFoldersDatabase.value?.let {
                if(it.isEmpty()) return@let

                it.forEach { folder ->
                    FolderUtils.copyFoldersTo(listOf(folder), destinationFolder, database, dataViewModel)

                    numberOfPastedItems++
                    _pasteProgressPercentage.value = numberOfPastedItems * 100 / totalNumberOfItems
                }

                if(!cutSelected) return@let

                database.folderDatabaseDao.deleteList(it)
            }

            dataViewModel.deleteUnusedImages()
            cutSelected = false
            _pasteInProgress.value = false
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

        if(_pasteInProgress.value == true) return

        _pasteProgressPercentage.value = 0
        _pasteInProgress.value = true

        var totalNumberOfFlashcards: Int = selectedFlashcardsDatabase.value?.size ?: 0

        //When percentage is 0 progress indicator is not visible
        //this will give indicator one more percentage increment that is visible at start
        totalNumberOfFlashcards++
        var numberOfPastedFlashcards = 1
        _pasteProgressPercentage.value = numberOfPastedFlashcards * 100 / totalNumberOfFlashcards

        viewModelScope.launch {
            selectedFlashcardsDatabase.value?.let {
                if(it.isEmpty()) return@let

                it.forEach { flashcard ->
                    FlashcardUtils.copyFlashcardsTo(listOf(flashcard), destinationFileId, database, dataViewModel)

                    numberOfPastedFlashcards++
                    _pasteProgressPercentage.value = numberOfPastedFlashcards * 100 / totalNumberOfFlashcards
                }

                if(!cutSelected) return@let

                database.flashcardDatabaseDao.deleteList(it)
            }

            dataViewModel.deleteUnusedImages()
            cutSelected = false
            _pasteInProgress.value = false
        }
    }
}