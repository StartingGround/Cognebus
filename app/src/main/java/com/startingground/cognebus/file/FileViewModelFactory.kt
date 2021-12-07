package com.startingground.cognebus.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class FileViewModelFactory(
    private val database: CognebusDatabase,
    private val fileId: Long,
    private val dataViewModel: DataViewModel
    ) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FileViewModel::class.java)){
            return FileViewModel(database, fileId, dataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}