package com.startingground.cognebus.flashcard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.startingground.cognebus.DataViewModel
import com.startingground.cognebus.R
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.database.entity.ImageDB

class Flashcard(private val database: CognebusDatabase,
                private val dataViewModel: DataViewModel?,
                fileId: Long
) {

    private var flashcardDB: FlashcardDB = FlashcardDB(0L, "", "", fileId)

    private val _questionText: MutableLiveData<String> = MutableLiveData("")
    val questionText: LiveData<String> get() = _questionText

    private val _answerText: MutableLiveData<String> = MutableLiveData("")
    val answerText: LiveData<String> get() = _answerText


    fun setFlashcard(flashcard: FlashcardDB){
        flashcardDB = flashcard
        _questionText.value = flashcardDB.question
        _answerText.value = flashcardDB.answer
    }



    fun setQuestionText(text: String){
        flashcardDB.question = text
        _questionText.value = text
    }

    fun setAnswerText(text: String){
        flashcardDB.answer = text
        _answerText.value = text
    }


    private val pendingImages = mutableListOf<Long>()


    suspend fun createImageInDatabase() : Long{
        val image = ImageDB(0L, null)
        val imageId = database.imageDatabaseDao.insert(image)
        pendingImages.add(imageId)
        return imageId
    }


    fun getIdOfLastCreatedImageInDatabase() : Long{
        return pendingImages.last()
    }


    fun deleteImages(){
        val pendingImagesDB = pendingImages.map { ImageDB(it, 0L) }
        dataViewModel?.deleteImages(pendingImagesDB)
    }


    fun thereIsExistingFlashcard(): Boolean {
        return flashcardDB.flashcardId != 0L
    }


    fun checkForErrors() : Pair<String?, String?>{
        val questionError = if (flashcardDB.question.isBlank()) {
            dataViewModel?.getStringFromResources(R.string.flashcard_question_fragment_question_text_empty_field_error) ?: "error"
        } else {
            null
        }

        val answerError = if (flashcardDB.answer.isBlank()) {
            dataViewModel?.getStringFromResources(R.string.flashcard_answer_fragment_answer_text_empty_field_error) ?: "error"
        } else {
            null
        }

        return Pair(questionError, answerError)
    }


    fun addToDatabase(){
        val (usedImages, unusedImages) = getUsedAndUnusedImages()
        val usedImagesDB = usedImages.map{ ImageDB(it, 0L) }
        val unusedImagesDB = unusedImages.map { ImageDB(it, 0L) }
        dataViewModel?.insertFlashcardToDatabaseAndUpdateUsedImagesInDatabaseAndDeleteUnused(flashcardDB, usedImagesDB, unusedImagesDB)
    }

    private var existingFlashcardImages = database.imageDatabaseDao.getImageIdsByFlashcardId(flashcardDB.flashcardId)

    fun updateInDatabase(){
        dataViewModel?.updateFlashcardInDatabase(flashcardDB)

        existingFlashcardImages.value?.let { pendingImages.addAll(it) }
        assignFlashcardIdToImagesAndRemoveUnusedImages()
    }


    private fun assignFlashcardIdToImagesAndRemoveUnusedImages(){
        if(pendingImages.isEmpty()) return

        val (usedImages, unusedImages) = getUsedAndUnusedImages()

        val usedImagesDB = usedImages.map{ ImageDB(it, flashcardDB.flashcardId) }
        dataViewModel?.updateImagesInDatabase(usedImagesDB)

        val unusedImagesDB = unusedImages.map { ImageDB(it, 0L) }
        dataViewModel?.deleteImages(unusedImagesDB)
    }


    private fun getUsedAndUnusedImages(): Pair<List<Long>, List<Long>>{
        return pendingImages.partition {
            flashcardDB.question.contains("src=\"$it\"") || flashcardDB.answer.contains("src=\"$it\"")
        }
    }
}