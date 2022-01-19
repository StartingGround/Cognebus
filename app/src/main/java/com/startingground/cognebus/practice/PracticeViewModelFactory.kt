package com.startingground.cognebus.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class PracticeViewModelFactory(
    private val assistedFactory: PracticeViewModelAssistedFactory,
    private val dataViewModel: DataViewModel
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)){
            return  assistedFactory.create(dataViewModel) as T
        }

        throw IllegalArgumentException("Unknown View Model class")
    }
}


@AssistedFactory
interface PracticeViewModelAssistedFactory{
    fun create(
        @Assisted dataViewModel: DataViewModel
    ): PracticeViewModel
}