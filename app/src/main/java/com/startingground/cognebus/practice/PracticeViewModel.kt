package com.startingground.cognebus.practice

import android.app.Application
import androidx.lifecycle.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.utilities.DataUtils
import com.startingground.cognebus.utilities.FlashcardUtils
import com.startingground.cognebus.utilities.MINIMAL_MAX_DAYS_PER_CYCLE
import com.startingground.cognebus.utilities.TimeCognebusUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    application: Application,
    private val dataUtils: DataUtils,
    private val flashcardUtils: FlashcardUtils
    ) : AndroidViewModel(application){

    private val database = CognebusDatabase.getInstance(application.applicationContext)

    private var flashcardsForPractice: MutableList<FlashcardDB> = mutableListOf()
    private var incorrectlyAnsweredFlashcards: MutableList<FlashcardDB> = mutableListOf()

    private var files: MutableMap<Long, FileDB> = mutableMapOf()

    private var inRepetitionMode: Boolean = false

    private val _flashcardNumber: MutableLiveData<Pair<Int,Int>> = MutableLiveData(0 to 0)
    val flashcardNumber: LiveData<Pair<Int, Int>> get() = _flashcardNumber

    private var totalNumberOfFlashcards = 0

    fun setFlashcards(flashcards: MutableList<FlashcardDB>, usedFiles: MutableList<FileDB>, repetitionMode: Boolean){
        inRepetitionMode = repetitionMode

        flashcardsForPractice.clear()
        incorrectlyAnsweredFlashcards.clear()
        files.clear()

        flashcardsForPractice.addAll(flashcards)
        totalNumberOfFlashcards = flashcardsForPractice.size

        val usedFilesPairs = usedFiles.map { it.fileId to it }

        files.putAll(usedFilesPairs)

        getNextFlashcard()
    }


    private val _currentFlashcard: MutableLiveData<FlashcardDB?> = MutableLiveData(null)
    val currentFlashcard: LiveData<FlashcardDB?> get() = _currentFlashcard


    private val _currentQuestionText: MediatorLiveData<String> = MediatorLiveData()

    init {
        _currentQuestionText.value = ""
        _currentQuestionText.addSource(_currentFlashcard){
            _currentQuestionText.value = ""
            viewModelScope.launch {
                val enableHTML = files[it?.fileId]?.enableHtml ?: false
                val text = it?.question ?: return@launch
                val imageList = database.imageDatabaseDao.getImagesByFlashcardId(it.flashcardId)
                _currentQuestionText.value = flashcardUtils.prepareStringForPractice(text, enableHTML, imageList)
            }
        }
    }

    val currentQuestionText: LiveData<String> get() = _currentQuestionText


    private val _currentAnswerText: MediatorLiveData<String> = MediatorLiveData()

    init {
        _currentAnswerText.value = ""
        _currentAnswerText.addSource(_currentFlashcard){
            _currentAnswerText.value = ""
            viewModelScope.launch {
                val enableHTML = files[it?.fileId]?.enableHtml ?: false
                val text = it?.answer ?: return@launch
                val imageList = database.imageDatabaseDao.getImagesByFlashcardId(it.flashcardId)
                _currentAnswerText.value = flashcardUtils.prepareStringForPractice(text, enableHTML, imageList)
            }
        }
    }

    val currentAnswerText: LiveData<String> get() = _currentAnswerText


    fun onCorrectAnswerButton(){
        _currentFlashcard.value?.let {
            if(inRepetitionMode){
                changeFlashcardRepetitionDateAndLastIncreaseOnCorrectAnswer(it)
            } else {
                it.answeredInPractice = true
            }
            dataUtils.updateFlashcardInDatabase(it)
        }
        getNextFlashcard()
    }


    fun onIncorrectAnswerButton(){
        _currentFlashcard.value?.let {
            incorrectlyAnsweredFlashcards.add(it)
        }
        getNextFlashcard()
    }


    private fun getNextFlashcard(){
        if(flashcardsForPractice.isNotEmpty()) {
            _currentFlashcard.value = flashcardsForPractice.removeFirst()

            val currentFlashcardNumber = totalNumberOfFlashcards - flashcardsForPractice.size
            _flashcardNumber.value = currentFlashcardNumber to totalNumberOfFlashcards

            return
        }

        if (incorrectlyAnsweredFlashcards.isNotEmpty()) {
            flashcardsForPractice.addAll(incorrectlyAnsweredFlashcards)
            incorrectlyAnsweredFlashcards.clear()
            _currentFlashcard.value = flashcardsForPractice.removeFirst()

            val currentFlashcardNumber = totalNumberOfFlashcards - flashcardsForPractice.size
            _flashcardNumber.value = currentFlashcardNumber to totalNumberOfFlashcards

            return
        }

        _currentFlashcard.value = null
    }


    private fun changeFlashcardRepetitionDateAndLastIncreaseOnCorrectAnswer(flashcard: FlashcardDB){
        files[flashcard.fileId]?.let{
            when(flashcard.lastIncrease){
                0 -> changeFlashcardsRepetitionDateAndLastIncrease(flashcard, 1)
                1 -> changeFlashcardsRepetitionDateAndLastIncrease(flashcard, 3 - 1)
                3 -> changeFlashcardsRepetitionDateAndLastIncrease(flashcard, 7 - 3)
                7 -> changeFlashcardsRepetitionDateAndLastIncrease(flashcard, 15 - 7)
                15 -> changeFlashcardsRepetitionDateAndLastIncrease(flashcard, 30 - 15)
                else -> changeFlashcardsRepetitionDateAndLastIncrease(flashcard, it.cycleIncrement, it.maxDaysPerCycle)
            }
        }
    }


    private fun changeFlashcardsRepetitionDateAndLastIncrease(flashcard: FlashcardDB, daysIncrease: Int, maxDaysPerCycle: Int = MINIMAL_MAX_DAYS_PER_CYCLE - 1){
        flashcard.lastIncrease += daysIncrease
        if(flashcard.lastIncrease > maxDaysPerCycle){
            flashcard.lastIncrease = maxDaysPerCycle
        }
        val time = TimeCognebusUtils.getBeginningOfCurrentDay()
        time.add(Calendar.DAY_OF_YEAR, flashcard.lastIncrease)
        flashcard.repetitionDate = time.timeInMillis
    }

    override fun onCleared() {
        super.onCleared()

        _currentAnswerText.removeSource(_currentFlashcard)
        _currentQuestionText.removeSource(_currentFlashcard)
    }
}