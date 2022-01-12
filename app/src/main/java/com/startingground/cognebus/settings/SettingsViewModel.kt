package com.startingground.cognebus.settings

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.startingground.cognebus.R
import com.startingground.cognebus.utilities.MINIMAL_CYCLE_INCREMENT
import com.startingground.cognebus.utilities.MINIMAL_MAX_DAYS_PER_CYCLE
import com.startingground.cognebus.utilities.StringUtils

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object{
        //File Default Options
        const val ENABLE_HTML_KEY = "enable_html"
        const val REPETITION_ENABLED_KEY = "repetition_enabled"
        const val CONTINUE_REPETITION_KEY = "continue_repetition"
        const val CYCLE_INCREMENT_KEY = "cycle_increment"
        const val MAX_DAYS_PER_CYCLE_KEY = "max_days_per_cycle"

        const val ENABLE_HTML_DEFAULT_VALUE = false
        const val REPETITION_ENABLED_DEFAULT_VALUE = true
        const val CONTINUE_REPETITION_DEFAULT_VALUE = false
        const val CYCLE_INCREMENT_DEFAULT_VALUE: Int = 15
        const val MAX_DAYS_PER_CYCLE_DEFAULT_VALUE: Int = 60

        //Flashcard Options
        const val CONSECUTIVE_FLASHCARD_CREATION_KEY = "consecutive_flashcard_creation"
        const val CROP_IMAGE_WHEN_ADDED_KEY = "crop_image_when_added"

        const val CONSECUTIVE_FLASHCARD_CREATION_DEFAULT_VALUE = false
        const val CROP_IMAGE_WHEN_ADDED_DEFAULT_VALUE = false
    }


    private fun switchValueChangedHandler(view: View, key: String, mutableLiveDataOfSwitch: MutableLiveData<Boolean>){
        if(view !is SwitchMaterial) return
        with(preferences.edit()){
            putBoolean(key, view.isChecked)
            apply()
        }
        mutableLiveDataOfSwitch.value = view.isChecked
    }


    private val preferences = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)


    //File Default Options
    private val _enableHTML: MutableLiveData<Boolean> = MutableLiveData(
        preferences.getBoolean(ENABLE_HTML_KEY, ENABLE_HTML_DEFAULT_VALUE)
    )
    val enableHTML: LiveData<Boolean> get() = _enableHTML

    fun onEnableHtmlChanged(view: View){
        switchValueChangedHandler(view, ENABLE_HTML_KEY, _enableHTML)
    }


    private val _repetitionEnabled: MutableLiveData<Boolean> = MutableLiveData(
        preferences.getBoolean(REPETITION_ENABLED_KEY, REPETITION_ENABLED_DEFAULT_VALUE)
    )
    val repetitionEnabled: LiveData<Boolean> get() = _repetitionEnabled

    fun onRepetitionEnabledChanged(view: View){
        switchValueChangedHandler(view, REPETITION_ENABLED_KEY, _repetitionEnabled)
    }


    private val _continueRepetition: MutableLiveData<Boolean> = MutableLiveData(
        preferences.getBoolean(CONTINUE_REPETITION_KEY, CONTINUE_REPETITION_DEFAULT_VALUE)
    )
    val continueRepetition: LiveData<Boolean> get() = _continueRepetition

    fun onContinueRepetitionChanged(view: View){
        switchValueChangedHandler(view, CONTINUE_REPETITION_KEY, _continueRepetition)
    }


    private val _cycleIncrement: MutableLiveData<Int> = MutableLiveData(
        preferences.getInt(CYCLE_INCREMENT_KEY, CYCLE_INCREMENT_DEFAULT_VALUE)
    )
    val cycleIncrement: LiveData<Int> get() = _cycleIncrement

    private val _cycleIncrementError: MutableLiveData<String?> = MutableLiveData(null)
    val cycleIncrementError: LiveData<String?> get() = _cycleIncrementError

    fun onCycleIncrementChanged(text: String){
        val context = getApplication<Application>().applicationContext
        val errorText = StringUtils.getErrorForInvalidIntegerValueInString(
            text,
            context.getString(R.string.file_fragment_cycle_increment_edit_text_invalid_input_error),
            MINIMAL_CYCLE_INCREMENT,
            context.getString(R.string.file_fragment_cycle_increment_edit_text_value_under_error, MINIMAL_CYCLE_INCREMENT)
        )
        _cycleIncrementError.value = errorText

        if(errorText != null) return

        val value = text.toInt()
        with(preferences.edit()){
            putInt(CYCLE_INCREMENT_KEY, value)
            apply()
        }
        _cycleIncrement.value = value
    }


    private val _maxDaysPerCycle: MutableLiveData<Int> = MutableLiveData(
        preferences.getInt(MAX_DAYS_PER_CYCLE_KEY, MAX_DAYS_PER_CYCLE_DEFAULT_VALUE)
    )
    val maxDaysPerCycle: LiveData<Int> get() = _maxDaysPerCycle

    private val _maxDaysPerCycleError: MutableLiveData<String?> = MutableLiveData(null)
    val maxDaysPerCycleError: LiveData<String?> get() = _maxDaysPerCycleError

    fun onMaxDaysPerCycleChanged(text: String){
        val context = getApplication<Application>().applicationContext
        val errorText = StringUtils.getErrorForInvalidIntegerValueInString(
            text,
            context.getString(R.string.file_fragment_max_days_per_cycle_edit_text_invalid_input_error),
            MINIMAL_MAX_DAYS_PER_CYCLE,
            context.getString(R.string.file_fragment_max_days_per_cycle_edit_text_value_under_error, MINIMAL_MAX_DAYS_PER_CYCLE)
        )
        _maxDaysPerCycleError.value = errorText

        if(errorText != null) return

        val value = text.toInt()
        with(preferences.edit()){
            putInt(MAX_DAYS_PER_CYCLE_KEY, value)
            apply()
        }
        _maxDaysPerCycle.value = value
    }


    //Flashcard Options
    private val _consecutiveFlashcardCreation: MutableLiveData<Boolean> = MutableLiveData(
        preferences.getBoolean(CONSECUTIVE_FLASHCARD_CREATION_KEY, CONSECUTIVE_FLASHCARD_CREATION_DEFAULT_VALUE)
    )
    val consecutiveFlashcardCreation: LiveData<Boolean> get() = _consecutiveFlashcardCreation

    fun onConsecutiveFlashcardCreationChanged(view: View){
        switchValueChangedHandler(view, CONSECUTIVE_FLASHCARD_CREATION_KEY, _consecutiveFlashcardCreation)
    }


    private val _cropImageWhenAdded: MutableLiveData<Boolean> = MutableLiveData(
        preferences.getBoolean(CROP_IMAGE_WHEN_ADDED_KEY, CROP_IMAGE_WHEN_ADDED_DEFAULT_VALUE)
    )
    val cropImageWhenAdded: LiveData<Boolean> get() = _cropImageWhenAdded

    fun onCropImageWhenAddedChanged(view: View){
        switchValueChangedHandler(view, CROP_IMAGE_WHEN_ADDED_KEY, _cropImageWhenAdded)
    }
}