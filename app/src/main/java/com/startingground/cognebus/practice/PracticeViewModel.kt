package com.startingground.cognebus.practice

import android.app.Application
import androidx.lifecycle.*
import com.startingground.cognebus.*
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.utilities.FlashcardUtils
import com.startingground.cognebus.utilities.MINIMAL_MAX_DAYS_PER_CYCLE
import java.util.*

class PracticeViewModel(application: Application, private val dataViewModel: DataViewModel) : AndroidViewModel(application){


    private var flashcardsForPractice: MutableList<FlashcardDB> = mutableListOf()
    private var incorrectlyAnsweredFlashcards: MutableList<FlashcardDB> = mutableListOf()

    private var files: MutableMap<Long, FileDB> = mutableMapOf()

    private var inRepetitionMode: Boolean = false

    fun setFlashcards(flashcards: MutableList<FlashcardDB>, usedFiles: MutableList<FileDB>, repetitionMode: Boolean){
        inRepetitionMode = repetitionMode

        flashcardsForPractice.clear()
        incorrectlyAnsweredFlashcards.clear()
        files.clear()

        flashcardsForPractice.addAll(flashcards)

        val usedFilesPairs = usedFiles.map { it.fileId to it }

        files.putAll(usedFilesPairs)

        getNextFlashcard()
    }


    private val _currentFlashcard: MutableLiveData<FlashcardDB?> = MutableLiveData(null)
    val currentFlashcard: LiveData<FlashcardDB?> get() = _currentFlashcard


    val currentQuestionText: LiveData<String> = Transformations.map(_currentFlashcard){
        val context = getApplication<Application>().applicationContext

        val enableHTML = files[it?.fileId]?.enableHtml ?: false
        val text = it?.question ?: return@map ""
        FlashcardUtils.prepareStringForPractice(context, text, enableHTML)
    }


    val currentAnswerText: LiveData<String> = Transformations.map(_currentFlashcard){
        val context = getApplication<Application>().applicationContext

        val enableHTML = files[it?.fileId]?.enableHtml ?: false
        val text = it?.answer ?: return@map ""
        FlashcardUtils.prepareStringForPractice(context, text, enableHTML)
    }


    fun onCorrectAnswerButton(){
        _currentFlashcard.value?.let {
            if(inRepetitionMode){
                changeFlashcardRepetitionDateAndLastIncreaseOnCorrectAnswer(it)
            } else {
                it.answeredInPractice = true
            }
            dataViewModel.updateFlashcardInDatabase(it)
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
            return
        }

        if (incorrectlyAnsweredFlashcards.isNotEmpty()) {
            flashcardsForPractice.addAll(incorrectlyAnsweredFlashcards)
            incorrectlyAnsweredFlashcards.clear()
            _currentFlashcard.value = flashcardsForPractice.removeFirst()
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
        val time = Calendar.getInstance()
        time.timeInMillis = flashcard.repetitionDate
        time.add(Calendar.DAY_OF_YEAR, flashcard.lastIncrease)
        flashcard.repetitionDate = time.timeInMillis
    }
}