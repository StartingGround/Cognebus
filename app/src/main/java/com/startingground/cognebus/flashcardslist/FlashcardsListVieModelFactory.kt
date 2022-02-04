package com.startingground.cognebus.flashcardslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class FlashcardsListVieModelFactory(
    private val assistedFactory: FlashcardsListViewModelAssistedFactory,
    private val fileId: Long,
    private val enableHtml: Boolean
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FlashcardsListViewModel::class.java)){
            return assistedFactory.create(fileId, enableHtml) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@AssistedFactory
interface FlashcardsListViewModelAssistedFactory{
    fun create(
        @Assisted fileId: Long,
        @Assisted enableHtml: Boolean
    ): FlashcardsListViewModel
}