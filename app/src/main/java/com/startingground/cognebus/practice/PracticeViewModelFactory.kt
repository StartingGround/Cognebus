package com.startingground.cognebus.practice

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel

@Suppress("UNCHECKED_CAST")
class PracticeViewModelFactory(
    private val application: Application,
    private val dataViewModel: DataViewModel
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PracticeViewModel::class.java)){
            return  PracticeViewModel(application, dataViewModel) as T
        }

        throw IllegalArgumentException("Unknown View Model class")
    }
}