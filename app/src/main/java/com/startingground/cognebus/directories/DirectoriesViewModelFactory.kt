package com.startingground.cognebus.directories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class DirectoriesViewModelFactory(
    private val database: CognebusDatabase,
    private val folderId: Long?,
    private val dataViewModel: DataViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(DirectoriesViewModel::class.java)){
            return DirectoriesViewModel(database, folderId, dataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}