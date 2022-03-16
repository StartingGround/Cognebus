package com.startingground.cognebus.flashcard

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.ImageDB
import com.startingground.cognebus.utilities.DataUtils
import com.startingground.cognebus.utilities.FileCognebusUtils
import com.startingground.cognebus.utilities.FlashcardUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

class FlashcardViewModel @AssistedInject constructor(
    @Assisted val fileId: Long,
    private val dataUtils: DataUtils?,
    private val database: CognebusDatabase,
    private val flashcardUtils: FlashcardUtils,
    private val fileCognebusUtils: FileCognebusUtils,
    private val applicationContext: Application
    ): ViewModel(){

    private var _flashcard: Flashcard? = null
    val flashcard: Flashcard? get() = _flashcard

    fun createNewFlashcard(){
        _flashcard = Flashcard(database, dataUtils, fileId)
    }

    fun setExistingFlashcard(flashcardId: Long){
        viewModelScope.launch {
            _flashcard = Flashcard(database, dataUtils, fileId)
            val flashcardDB: FlashcardDB = database.flashcardDatabaseDao.getFlashcardByFlashcardId(flashcardId)
            _flashcard?.setFlashcard(flashcardDB)
        }
    }


    fun clearDataOnLeavingWithoutAddingFlashcard(){
        _flashcard?.deleteImages()
        _flashcard = null
        clearPreview()
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


    private val _getImageFromGalleryTrigger: MutableLiveData<IntentCaller> = MutableLiveData(IntentCaller.NONE)
    val getImageFromGalleryTrigger: LiveData<IntentCaller> get() = _getImageFromGalleryTrigger

    fun getImageFromGallery(caller: IntentCaller){
        viewModelScope.launch {
            _flashcard?.createImageInDatabase("holder")
            _getImageFromGalleryTrigger.value = caller
        }
    }


    fun saveImageToFileFromGalleryImageUri(galleryImageUri: Uri): Boolean{
        val fileExtension = fileCognebusUtils.getFileExtensionFromExternalUri(galleryImageUri) ?: return false
        val image = getAddedImage()
        image.fileExtension = fileExtension
        dataUtils?.updateImagesInDatabase(listOf(image))
        val imageFile = fileCognebusUtils.createFileOrGetExisting("images", "${image.imageId}.${image.fileExtension}") ?: return false

        dataUtils?.copyFileFromUri(galleryImageUri, imageFile)
        return true
    }


    private val _getImageFromCameraTrigger: MutableLiveData<Pair<IntentCaller, Uri?>> = MutableLiveData(IntentCaller.NONE to null)
    val getImageFromCameraTrigger: LiveData<Pair<IntentCaller, Uri?>> get() = _getImageFromCameraTrigger


    fun getImageFromCamera(caller: IntentCaller){
        viewModelScope.launch {
            _flashcard?.createImageInDatabase("jpg")
            val image = getAddedImage()
            val imageFile = fileCognebusUtils.createFileOrGetExisting("images", "${image.imageId}.${image.fileExtension}") ?: return@launch
            val imageUri = FileProvider.getUriForFile(applicationContext, "com.startingground.cognebus", imageFile)

            _getImageFromCameraTrigger.value = caller to imageUri
        }
    }


    fun clearIntentTriggers(){
        _getImageFromGalleryTrigger.value = IntentCaller.NONE
        _getImageFromCameraTrigger.value = IntentCaller.NONE to null
    }


    fun getAddedImage(): ImageDB{
        return _flashcard?.getLastCreatedImageInDatabase() ?: throw Exception("getAddedImageId can't be executed!")
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
        clearPreview()
        return true
    }


    val showDollarSignAlert: Boolean get() = dataUtils?.getShowDollarSignAlert() ?: true


    fun disableDollarSignAlert(){
        dataUtils?.setShowDollarSignAlert(false)
    }


    //Preview ---------------------------------------------------------------------
    private val _previewModeIsEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
    val previewModeIsEnabled: LiveData<Boolean> get() = _previewModeIsEnabled


    fun onPreviewButton(){
        _previewModeIsEnabled.value = _previewModeIsEnabled.value?.not() ?: false
        if(_previewModeIsEnabled.value != true) return

        prepareFlashcardForPreview()
    }

    private fun clearPreview(){
        _previewModeIsEnabled.value = false
        _questionPreviewText.value = ""
        _answerPreviewText.value = ""
    }

    private val fileDB = database.fileDatabaseDao.getLiveDataFileByFileId(fileId)

    private val _questionPreviewText: MutableLiveData<String> = MutableLiveData("")
    val questionPreviewText: LiveData<String> get() = _questionPreviewText

    private val _answerPreviewText: MutableLiveData<String> = MutableLiveData("")
    val answerPreviewText: LiveData<String> get() = _answerPreviewText


    private fun prepareFlashcardForPreview(){
        val enableHTML = fileDB?.value?.enableHtml ?: false

        val imageList: List<ImageDB> = _flashcard?.getImageListForPreview() ?: listOf()

        val questionText = _flashcard?.questionText?.value ?: ""
        _questionPreviewText.value = flashcardUtils.prepareStringForPractice(questionText, enableHTML, imageList)

        val answerText = _flashcard?.answerText?.value ?: ""
        _answerPreviewText.value = flashcardUtils.prepareStringForPractice(answerText, enableHTML, imageList)
    }
}

enum class IntentCaller{ NONE, QUESTION, ANSWER }