package com.startingground.cognebus.createorrenamefolderorfile

import android.app.Application
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.startingground.cognebus.utilities.DataUtils
import com.startingground.cognebus.R
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.directories.DirectoriesFragment
import com.startingground.cognebus.settings.SettingsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

class CreateOrRenameViewModel @AssistedInject constructor(
    @Assisted("folderId") private val folderId: Long?,
    @Assisted val inputType: Int,
    @Assisted("existingFolderOrFileId") val existingFolderOrFileId: Long?,
    private val dataUtils: DataUtils,
    private val database: CognebusDatabase,
    application: Application,
) : AndroidViewModel(application) {

    private val _fileOrFolderText: MutableLiveData<String> = MutableLiveData("")
    val fileOrFolderText: LiveData<String> get() = _fileOrFolderText

    fun addFileOrFolderText(text: String){
        _fileOrFolderText.value = text
    }


    private var currentFileOrFolderText: String = ""

    init {
        if(existingFolderOrFileId != null) {
            viewModelScope.launch {

                when (inputType) {

                    DirectoriesFragment.TYPE_FOLDER -> {
                        val folder = database.folderDatabaseDao.getFolderByFolderId(existingFolderOrFileId)
                        currentFileOrFolderText = folder.name
                    }

                    DirectoriesFragment.TYPE_FILE -> {
                        val file = database.fileDatabaseDao.getFileByFileId(existingFolderOrFileId)
                        currentFileOrFolderText = file?.name ?: return@launch
                    }

                }
                addFileOrFolderText(currentFileOrFolderText)

            }
        }
    }


    fun onCreateOrRenameButton(){
        when(inputType){
            DirectoriesFragment.TYPE_FOLDER -> createOrRenameFolder()
            DirectoriesFragment.TYPE_FILE -> createOrRenameFile()
        }
    }


    private val _fileOrFolderErrorText: MutableLiveData<String?> = MutableLiveData(null)
    val fileOrFolderErrorText: LiveData<String?> get() = _fileOrFolderErrorText

    private val _goBackToDirectoriesTrigger: MutableLiveData<Boolean> = MutableLiveData(false)
    val goBackToDirectoriesTrigger: LiveData<Boolean> get() = _goBackToDirectoriesTrigger

    private fun createOrRenameFolder(){
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            val text = _fileOrFolderText.value?.trim()
            if (text.isNullOrEmpty()) {
                _fileOrFolderErrorText.value = context.getString(R.string.create_or_rename_folder_or_file_fragment_invalid_input_error)
                return@launch
            }

            val existingFolder = database.folderDatabaseDao.getFolderByParentFolderIdAndName(folderId, text)
            if (existingFolder != null && existingFolder.folderId != existingFolderOrFileId) {
                _fileOrFolderErrorText.value = context.getString(R.string.create_or_rename_folder_or_file_fragment_already_exists_error)
                return@launch
            }

            if(existingFolderOrFileId == null) {
                val folder = Folder(0, text, folderId)
                dataUtils.insertFolderToDatabase(folder)
            } else{
                val folder = Folder(existingFolderOrFileId, text, folderId)
                database.folderDatabaseDao.update(folder)
            }
            _goBackToDirectoriesTrigger.value = true
        }
    }


    private fun createOrRenameFile(){
        val context = getApplication<Application>().applicationContext

        viewModelScope.launch {
            val text = _fileOrFolderText.value?.trim()
            if (text.isNullOrEmpty()) {
                _fileOrFolderErrorText.value = context.getString(R.string.create_or_rename_folder_or_file_fragment_invalid_input_error)
                return@launch
            }

            val existingFile = database.fileDatabaseDao.getFileByFolderIdAndName(folderId, text)
            if (existingFile != null && existingFile.fileId != existingFolderOrFileId) {
                _fileOrFolderErrorText.value = context.getString(R.string.create_or_rename_folder_or_file_fragment_already_exists_error)
                return@launch
            }

            if(existingFolderOrFileId == null) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                val file = FileDB(
                    0L,
                    text,
                    folderId,
                    1L,
                    preferences.getBoolean(
                        SettingsViewModel.ENABLE_HTML_KEY,
                        SettingsViewModel.ENABLE_HTML_DEFAULT_VALUE
                    ),
                    preferences.getBoolean(
                        SettingsViewModel.ONLY_PRACTICE_ENABLED_KEY,
                        SettingsViewModel.ONLY_PRACTICE_ENABLED_DEFAULT_VALUE
                    ),
                    preferences.getBoolean(
                        SettingsViewModel.REPETITION_ENABLED_KEY,
                        SettingsViewModel.REPETITION_ENABLED_DEFAULT_VALUE
                    ),
                    preferences.getBoolean(
                        SettingsViewModel.CONTINUE_REPETITION_KEY,
                        SettingsViewModel.CONTINUE_REPETITION_DEFAULT_VALUE
                    ),
                    preferences.getInt(
                        SettingsViewModel.CYCLE_INCREMENT_KEY,
                        SettingsViewModel.CYCLE_INCREMENT_DEFAULT_VALUE
                    ),
                    preferences.getInt(
                        SettingsViewModel.MAX_DAYS_PER_CYCLE_KEY,
                        SettingsViewModel.MAX_DAYS_PER_CYCLE_DEFAULT_VALUE
                    )
                )
                dataUtils.insertFileToDatabase(file)
            } else{
                var file = database.fileDatabaseDao.getFileByFileId(existingFolderOrFileId)
                file = file?.copy(name = text)
                file?.let {
                    database.fileDatabaseDao.update(it)
                }
            }

            _goBackToDirectoriesTrigger.value = true
        }
    }

    fun removeErrorText(){
        _fileOrFolderErrorText.value = null
    }
}