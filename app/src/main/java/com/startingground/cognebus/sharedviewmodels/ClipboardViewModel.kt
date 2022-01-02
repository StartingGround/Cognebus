package com.startingground.cognebus.sharedviewmodels

import androidx.lifecycle.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.utilities.FileDBUtils
import com.startingground.cognebus.utilities.FolderUtils
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
}