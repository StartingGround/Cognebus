package com.startingground.cognebus.file

import android.content.Context
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import com.startingground.cognebus.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.Sorting
import com.startingground.cognebus.utilities.DataUtils
import com.startingground.cognebus.utilities.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext

class FileViewModel @AssistedInject constructor(
    @Assisted fileId: Long,
    private val dataUtils: DataUtils,
    @ApplicationContext context: Context
    ): ViewModel(){

    private val database = CognebusDatabase.getInstance(context)

    private var _sortingList: LiveData<List<Sorting>> = database.sortingDatabaseDao.getAll()
    val sortingList: LiveData<Array<String>> = Transformations.map(_sortingList){
        Array(it.size){index ->
            it[index].sortingType
        }
    }

    private val _file: LiveData<FileDB> = database.fileDatabaseDao.getLiveDataFileByFileId(fileId) ?: MutableLiveData()
    val file: LiveData<FileDB> get() = _file

    private val _flashcards: LiveData<List<FlashcardDB>> = database.flashcardDatabaseDao.getLiveDataFlashcardsByFileId(fileId)

    val fileContainsFlashcards: LiveData<Boolean> = Transformations.map(_flashcards){
        it.isNotEmpty()
    }

    val numberOfFlashcardsForPractice: LiveData<Pair<Int, Int>> = Transformations.map(_flashcards){ flashcards ->
        var flashcardList = flashcards

        if(_file.value?.onlyPracticeEnabled == true){
            flashcardList = flashcardList.filter{ it.repetitionEnabled }
        }

        val totalNumberOfFlashcards: Int = flashcardList.size
        var numberOfFlashcardsForPractice: Int = flashcardList.filter { !it.answeredInPractice }.size
        if (numberOfFlashcardsForPractice == 0) numberOfFlashcardsForPractice = totalNumberOfFlashcards

        Pair(numberOfFlashcardsForPractice, totalNumberOfFlashcards)
    }


    fun onSortingSelected(sortingId: Long){
        _file.value?.sortingId = sortingId + 1
        updateFile()
    }


    fun onEnableHtmlChanged(view: View){
        if(view !is SwitchMaterial) return
        _file.value?.enableHtml = view.isChecked
        updateFile()
    }


    fun onOnlyPracticeEnabledChanged(view: View){
        if(view !is SwitchMaterial) return
        _file.value?.onlyPracticeEnabled = view.isChecked
        updateFile()

        //Needed to make _flashcards look like they changed to trigger
        // update of other transformations of _flashcards liveData
        _flashcards.value?.let {
            if(it.isEmpty()) return@let
            dataUtils.updateFlashcardInDatabase(it.first())
        }
    }


    fun onRepetitionChanged(view: View){
        if(view !is SwitchMaterial) return
        _file.value?.repetitionEnabled = view.isChecked
        updateFile()
    }


    fun onContinueRepetitionChanged(view: View){
        if(view !is SwitchMaterial) return
        _file.value?.continueRepetitionAfterDefaultPeriod = view.isChecked
        updateFile()
    }


    private val _cycleIncrementError: MutableLiveData<String?> = MutableLiveData(null)
    val cycleIncrementError: LiveData<String?> get() = _cycleIncrementError


    fun onCycleIncrementChanged(text: String){
        if(_file.value == null) return

        val errorText = StringUtils.getErrorForInvalidIntegerValueInString(
            text,
            dataUtils.getStringFromResources(R.string.file_fragment_cycle_increment_edit_text_invalid_input_error),
            MINIMAL_CYCLE_INCREMENT,
            dataUtils.getStringFromResources(R.string.file_fragment_cycle_increment_edit_text_value_under_error).format(MINIMAL_CYCLE_INCREMENT)
        )
        _cycleIncrementError.value = errorText

        if(errorText != null) return

        val value = text.toInt()
        _file.value?.cycleIncrement = value
        updateFile()
    }


    private val _maxDaysPerCycleError: MutableLiveData<String?> = MutableLiveData(null)
    val maxDaysPerCycleError: LiveData<String?> get() = _maxDaysPerCycleError


    fun onMaxDaysPerCycleChanged(text: String){
        if(_file.value == null) return

        val errorText = StringUtils.getErrorForInvalidIntegerValueInString(
            text,
            dataUtils.getStringFromResources(R.string.file_fragment_max_days_per_cycle_edit_text_invalid_input_error),
            MINIMAL_MAX_DAYS_PER_CYCLE,
            dataUtils.getStringFromResources(R.string.file_fragment_max_days_per_cycle_edit_text_value_under_error).format(MINIMAL_MAX_DAYS_PER_CYCLE)
        )
        _maxDaysPerCycleError.value = errorText

        if(errorText != null) return

        val value = text.toInt()
        _file.value?.maxDaysPerCycle = value
        updateFile()
    }


    private fun updateFile(){
        file.value?.let {
            dataUtils.updateFileInDatabase(it)
        }
    }


    fun getFilesForPractice(): MutableList<FileDB>{
        val files: MutableList<FileDB> = mutableListOf()
        file.value?.let { files.add(it) }
        return files
    }


    fun getFlashcardsForPractice(): MutableList<FlashcardDB> {

        val flashcardsForPractice = mutableListOf<FlashcardDB>()

        _flashcards.value?.let { flashcards ->
            var flashcardsInUse = flashcards
            if(_file.value?.onlyPracticeEnabled == true){
                flashcardsInUse = flashcardsInUse.filter { it.repetitionEnabled }
            }

            val unansweredFlashcards = flashcardsInUse.filter { !it.answeredInPractice }

            flashcardsForPractice.addAll(unansweredFlashcards)

            if(unansweredFlashcards.isNotEmpty()) return@let

            changeFlashcardsToUnansweredInPracticeAndUpdateInDatabase(flashcardsInUse)
            flashcardsForPractice.addAll(flashcardsInUse)
        }

        when(_file.value?.sortingId){
                SORTING_IN_ORDER -> flashcardsForPractice.sortBy { it.flashcardId }
                SORTING_REVERSE_ORDER -> flashcardsForPractice.sortByDescending { it.flashcardId }
                SORTING_SHUFFLED -> flashcardsForPractice.shuffle()
            }

        return flashcardsForPractice
    }


    private fun changeFlashcardsToUnansweredInPracticeAndUpdateInDatabase(flashcards: List<FlashcardDB>){
        flashcards.forEach { it.answeredInPractice = false }
        dataUtils.updateFlashcardsInDatabase(flashcards)
    }
}