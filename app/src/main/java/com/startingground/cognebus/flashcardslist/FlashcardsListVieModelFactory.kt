package com.startingground.cognebus.flashcardslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class FlashcardsListVieModelFactory(
    private val assistedFactory: FlashcardsListViewModelAssistedFactory,
    private val fileId: Long,
    private val dataViewModel: DataViewModel,
    private val enableHtml: Boolean
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FlashcardsListViewModel::class.java)){
            return assistedFactory.create(fileId, dataViewModel, enableHtml) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@AssistedFactory
interface FlashcardsListViewModelAssistedFactory{
    fun create(
        @Assisted fileId: Long,
        @Assisted dataViewModel: DataViewModel,
        @Assisted enableHtml: Boolean
    ): FlashcardsListViewModel
}