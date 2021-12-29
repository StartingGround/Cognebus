package com.startingground.cognebus.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class FlashcardViewModelFactory(
    private val database: CognebusDatabase,
    private val fileId: Long,
    private val dataViewModel: DataViewModel?
    ): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            return FlashcardViewModel(database, fileId, dataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}