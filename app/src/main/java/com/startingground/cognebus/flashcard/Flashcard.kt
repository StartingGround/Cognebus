package com.startingground.cognebus.flashcard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.startingground.cognebus.utilities.DataUtils
import com.startingground.cognebus.R
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.ImageDB

class Flashcard(private val database: CognebusDatabase,
                private val dataUtils: DataUtils?,
                fileId: Long
) {

    private var flashcardDB: FlashcardDB = FlashcardDB(0L, "", "", fileId)

    private val _questionText: MutableLiveData<String> = MutableLiveData("")
    val questionText: LiveData<String> get() = _questionText

    private val _answerText: MutableLiveData<String> = MutableLiveData("")
    val answerText: LiveData<String> get() = _answerText

    private var existingFlashcardImages: List<ImageDB> = listOf()

    suspend fun setFlashcard(flashcard: FlashcardDB){
        flashcardDB = flashcard
        _questionText.value = flashcardDB.question
        _answerText.value = flashcardDB.answer
        existingFlashcardImages = database.imageDatabaseDao.getImagesByFlashcardId(flashcard.flashcardId)
    }



    fun setQuestionText(text: String){
        flashcardDB.question = text
        _questionText.value = text
    }

    fun setAnswerText(text: String){
        flashcardDB.answer = text
        _answerText.value = text
    }


    private val pendingImages = mutableListOf<ImageDB>()


    suspend fun createImageInDatabase(fileExtension: String){
        var image = ImageDB(0L, null, fileExtension)
        val imageId = database.imageDatabaseDao.insert(image)
        image = database.imageDatabaseDao.getImageByImageId(imageId)
        pendingImages.add(image)
    }


    fun getLastCreatedImageInDatabase() : ImageDB{
        return pendingImages.last()
    }


    fun deleteImages(){
        dataUtils?.deleteImages(pendingImages)
    }


    fun thereIsExistingFlashcard(): Boolean {
        return flashcardDB.flashcardId != 0L
    }


    fun checkForErrors() : Pair<String?, String?>{
        val questionError = if (flashcardDB.question.isBlank()) {
            dataUtils?.getStringFromResources(R.string.flashcard_question_fragment_question_text_empty_field_error) ?: "error"
        } else {
            null
        }

        val answerError = if (flashcardDB.answer.isBlank()) {
            dataUtils?.getStringFromResources(R.string.flashcard_answer_fragment_answer_text_empty_field_error) ?: "error"
        } else {
            null
        }

        return Pair(questionError, answerError)
    }


    fun addToDatabase(){
        val (usedImages, unusedImages) = getUsedAndUnusedImages()
        dataUtils?.insertFlashcardToDatabaseAndUpdateUsedImagesInDatabaseAndDeleteUnused(flashcardDB, usedImages, unusedImages)
    }


    fun updateInDatabase(){
        dataUtils?.updateFlashcardInDatabase(flashcardDB)

        pendingImages.addAll(existingFlashcardImages)
        assignFlashcardIdToImagesAndRemoveUnusedImages()
    }


    private fun assignFlashcardIdToImagesAndRemoveUnusedImages(){
        if(pendingImages.isEmpty()) return

        val (usedImages, unusedImages) = getUsedAndUnusedImages()

        val usedImagesDB = usedImages.map{ it.copy(flashcardId = flashcardDB.flashcardId) }
        dataUtils?.updateImagesInDatabase(usedImagesDB)

        dataUtils?.deleteImages(unusedImages)
    }


    private fun getUsedAndUnusedImages(): Pair<List<ImageDB>, List<ImageDB>>{
        return pendingImages.partition {
            flashcardDB.question.contains("src=\"${it.imageId}\"") || flashcardDB.answer.contains("src=\"${it.imageId}\"")
        }
    }

    fun getImageListForPreview(): List<ImageDB>{
        val imageList: MutableList<ImageDB> = mutableListOf()
        imageList.addAll(pendingImages)
        imageList.addAll(existingFlashcardImages)
        return imageList
    }


    fun changeImageFileExtension(imageId: Long, fileExtension: String){
        var image: ImageDB? = pendingImages.find { it.imageId == imageId }
        image = existingFlashcardImages.find { it.imageId == imageId } ?: image

        image?.let {
            it.fileExtension = fileExtension
            dataUtils?.updateImagesInDatabase(listOf(it))
        }
    }
}