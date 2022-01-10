package com.startingground.cognebus.flashcard

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.startingground.cognebus.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.ImageDB
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.utilities.FileCognebusUtils
import com.startingground.cognebus.utilities.FlashcardUtils
import kotlinx.coroutines.launch

class FlashcardViewModel(
    private val database: CognebusDatabase,
    private val fileId: Long,
    private val dataViewModel: DataViewModel?,
    app: Application
    ): AndroidViewModel(app){

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


    fun getImageFromGallery(fragment: Fragment){
        viewModelScope.launch {
            _flashcard?.createImageInDatabase("holder")
            sendIntentToGetImageFromGallery(fragment)
        }
    }

    private fun sendIntentToGetImageFromGallery(fragment: Fragment){
        if(fragment is IntentInterface){
            fragment.getImageFromGallery.launch("image/*")
        }
    }

    fun saveImageToFileFromGalleryImageUri(galleryImageUri: Uri): Boolean{
        val context = getApplication<Application>().applicationContext

        val fileExtension = FileCognebusUtils.getFileExtensionFromExternalUri(galleryImageUri, context) ?: return false
        val image = getAddedImage()
        image.fileExtension = fileExtension
        dataViewModel?.updateImagesInDatabase(listOf(image))
        val imageFile = dataViewModel?.createImageFileOrGetExisting(image.imageId, image.fileExtension) ?: return false

        dataViewModel.copyFileFromUri(galleryImageUri, imageFile)
        return true
    }


    fun getImageFromCamera(fragment: Fragment){
        viewModelScope.launch {
            _flashcard?.createImageInDatabase("jpg")
            val image = getAddedImage()
            val imageFile = dataViewModel?.createImageFileOrGetExisting(image.imageId, image.fileExtension) ?: return@launch
            val imageUri = FileProvider.getUriForFile(fragment.requireContext(), "com.startingground.cognebus", imageFile)
            sendIntentToTakePicture(imageUri, fragment)
        }
    }

    private fun sendIntentToTakePicture(imageUri: Uri, fragment: Fragment){
        if(fragment is IntentInterface){
            fragment.getImageFromCamera.launch(imageUri)
        }
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


    val showDollarSignAlert: Boolean get() = dataViewModel?.getShowDollarSignAlert() ?: true


    fun disableDollarSignAlert(){
        dataViewModel?.setShowDollarSignAlert(false)
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

    private val fileDB = database.fileDatabaseDao.getFileByFileId(fileId)

    private val _questionPreviewText: MutableLiveData<String> = MutableLiveData("")
    val questionPreviewText: LiveData<String> get() = _questionPreviewText

    private val _answerPreviewText: MutableLiveData<String> = MutableLiveData("")
    val answerPreviewText: LiveData<String> get() = _answerPreviewText


    private fun prepareFlashcardForPreview(){
        val context = getApplication<Application>().applicationContext

        val enableHTML = fileDB?.value?.enableHtml ?: false

        val imageList: List<ImageDB> = _flashcard?.getImageListForPreview() ?: listOf()

        val questionText = _flashcard?.questionText?.value ?: ""
        _questionPreviewText.value = FlashcardUtils.prepareStringForPractice(context, questionText, enableHTML, imageList)

        val answerText = _flashcard?.answerText?.value ?: ""
        _answerPreviewText.value = FlashcardUtils.prepareStringForPractice(context, answerText, enableHTML, imageList)
    }
}