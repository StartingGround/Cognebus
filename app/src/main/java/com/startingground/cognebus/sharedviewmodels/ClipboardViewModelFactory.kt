package com.startingground.cognebus.sharedviewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class ClipboardViewModelFactory(
    private val assistedFactory: ClipboardViewModelAssistedFactory,
    private val dataViewModel: DataViewModel
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ClipboardViewModel::class.java)){
            return assistedFactory.create(dataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@AssistedFactory
interface ClipboardViewModelAssistedFactory{
    fun create(
        @Assisted dataViewModel: DataViewModel
    ): ClipboardViewModel
}