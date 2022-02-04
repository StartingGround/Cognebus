package com.startingground.cognebus.flashcard

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class FlashcardViewModelFactory(
    private val assistedFactory: FlashcardViewModelAssistedFactory,
    private val fileId: Long,
    private val app: Application
    ): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            return assistedFactory.create(fileId, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@AssistedFactory
interface FlashcardViewModelAssistedFactory{
    fun create(
        @Assisted fileId: Long,
        @Assisted app: Application
    ): FlashcardViewModel
}