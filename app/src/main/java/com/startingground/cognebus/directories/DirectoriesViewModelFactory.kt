package com.startingground.cognebus.directories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class DirectoriesViewModelFactory(
    private val assistedFactory: DirectoriesViewModelAssistedFactory,
    private val folderId: Long?,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(DirectoriesViewModel::class.java)){
            return assistedFactory.create(folderId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@AssistedFactory
interface DirectoriesViewModelAssistedFactory{
    fun create(
        @Assisted folderId: Long?,
    ): DirectoriesViewModel
}