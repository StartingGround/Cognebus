package com.startingground.cognebus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class ClipboardViewModelFactory(private val database: CognebusDatabase, private val dataViewModel: DataViewModel) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(ClipboardViewModel::class.java)){
            return ClipboardViewModel(database, dataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}