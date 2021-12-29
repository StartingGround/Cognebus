package com.startingground.cognebus.flashcard

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startingground.cognebus.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import kotlinx.coroutines.launch

class FlashcardViewModel(
    private val database: CognebusDatabase,
    private val fileId: Long,
    private val dataViewModel: DataViewModel?
    ): ViewModel(){

    private var _flashcard: Flashcard? = null
    val flashcard: Flashcard? get() = _flashcard

    fun createNewFlashcard(){
        _flashcard = Flashcard(database, dataViewModel, fileId)
    }

    fun setExistingFlashcard(flashcardId: Long){
        viewModelScope.launch {
            _flashcard = Flashcard(database, dataViewModel, fileId)
            val flashcardDB: FlashcardDB = database.flashcardDatabaseDao.getFlashcardByFlashcardId(flashcardId)
            _flashcard?.setFlashcard(flashcardDB)
        }
    }


    fun clearDataOnLeavingWithoutAddingFlashcard(){
        _flashcard?.deleteImages()
        _flashcard = null
    }


    private var textCursorPositionStart = 0
    private var textCursorPositionEnd = 0

    fun saveTextCursorPositions(start: Int, end: Int){
        textCursorPositionStart = start
        textCursorPositionEnd = end
    }

    fun getTextCursorPositions(): Pair<Int, Int>{
        return Pair(textCursorPositionStart, textCursorPositionEnd)
    }


    fun getImageFromGallery(fragment: Fragment){
        viewModelScope.launch {
            _flashcard?.createImageInDatabase() ?: return@launch
            sendIntentToGetImageFromGallery(fragment)
        }
    }

    private fun sendIntentToGetImageFromGallery(fragment: Fragment){
        if(fragment is IntentInterface){
            fragment.getImageFromGallery.launch("image/jpeg")
        }
    }

    fun saveImageToFileFromGalleryImageUri(galleryImageUri: Uri): Boolean{
        val imageId = getAddedImageId()
        val imageFile = dataViewModel?.createImageFileOrGetExisting(imageId) ?: return false

        dataViewModel.copyFileFromUri(galleryImageUri, imageFile)
        return true
    }


    fun getImageFromCamera(fragment: Fragment){
        viewModelScope.launch {
            val imageId = _flashcard?.createImageInDatabase() ?: return@launch
            val imageFile = dataViewModel?.createImageFileOrGetExisting(imageId) ?: return@launch
            val imageUri = FileProvider.getUriForFile(fragment.requireContext(), "com.startingground.cognebus", imageFile)
            sendIntentToTakePicture(imageUri, fragment)
        }
    }

    private fun sendIntentToTakePicture(imageUri: Uri, fragment: Fragment){
        if(fragment is IntentInterface){
            fragment.getImageFromCamera.launch(imageUri)
        }
    }


    fun getAddedImageId(): Long{
        return _flashcard?.getIdOfLastCreatedImageInDatabase() ?: throw Exception("getAddedImageId can't be executed!")
    }


    private val _questionError: MutableLiveData<String?> = MutableLiveData(null)
    val questionError: LiveData<String?> get() = _questionError

    private val _answerError: MutableLiveData<String?> = MutableLiveData(null)
    val answerError: LiveData<String?> get() = _answerError


    fun addQuestionText(text: String){
        _flashcard?.setQuestionText(text)
        _questionError.value = null
    }


    fun addAnswerText(text: String){
        _flashcard?.setAnswerText(text)
        _answerError.value = null
    }


    fun saveFlashcardToDatabase(): Boolean{
        val (questionError, answerError) = _flashcard?.checkForErrors() ?: return false

        if(questionError != null || answerError != null){
            _questionError.value = questionError
            _answerError.value = answerError
            return false
        }

        val thereIsExistingFlashcard = _flashcard?.thereIsExistingFlashcard() ?: false

        if(thereIsExistingFlashcard) {
            _flashcard?.updateInDatabase()
        }
        else {
            _flashcard?.addToDatabase()
        }

        _flashcard = null
        return true
    }


    val showDollarSignAlert: Boolean get() = dataViewModel?.getShowDollarSignAlert() ?: true


    fun disableDollarSignAlert(){
        dataViewModel?.setShowDollarSignAlert(false)
    }
}