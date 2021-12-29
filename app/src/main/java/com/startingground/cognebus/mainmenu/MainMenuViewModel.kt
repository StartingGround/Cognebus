package com.startingground.cognebus.mainmenu

import androidx.lifecycle.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.utilities.TimeCognebusUtils
import kotlinx.coroutines.launch

class MainMenuViewModel(private val database: CognebusDatabase): ViewModel() {
    private var flashcardsForRepetition:  LiveData<List<FlashcardDB>>? = null

    fun reloadFlashcardsForRepetition(){
        //If we don't have this and app stays in background and we open it next day, then flashcards for repetition
        //will not be loaded
        flashcardsForRepetition?.removeObserver(flashcardsForRepetitionObserver)
        _readyForRepetition.value = false

        flashcardsForRepetition = database.flashcardDatabaseDao.getFlashcardsForRepetition(TimeCognebusUtils.getBeginningOfNextDayInMilliseconds())
        flashcardsForRepetition?.observeForever(flashcardsForRepetitionObserver)
    }

    fun getFlashcardsForRepetition(): List<FlashcardDB> {
        return flashcardsForRepetition?.value ?: listOf()
    }


    private val _readyForRepetition: MutableLiveData<Boolean> = MutableLiveData(false)
    val readyForRepetition: LiveData<Boolean> get() = _readyForRepetition

    private val filesForRepetition: MutableLiveData<List<FileDB>> = MutableLiveData(mutableListOf())


    private val flashcardsForRepetitionObserver = Observer<List<FlashcardDB>> { flashcards ->
        _readyForRepetition.value = false
        val filesToGet = flashcards.map { it.fileId }
        if(filesToGet.isEmpty()) return@Observer

        viewModelScope.launch {
            val files = database.fileDatabaseDao.getFilesInFileIdList(filesToGet)
            if(files.isEmpty()) return@launch

            filesForRepetition.value = files
            _readyForRepetition.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        flashcardsForRepetition?.removeObserver(flashcardsForRepetitionObserver)
    }


    fun getFilesForRepetition(): List<FileDB>{
        return filesForRepetition.value ?: listOf()
    }
}