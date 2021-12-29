package com.startingground.cognebus.sharedviewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class DataViewModelFactory(private val app: Application) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(DataViewModel::class.java)){
            return DataViewModel(app) as T
        }

        throw IllegalArgumentException("Unknown view model class")
    }
}