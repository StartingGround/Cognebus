package com.startingground.cognebus.flashcardslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class FlashcardsListVieModelFactory(
    private val database: CognebusDatabase,
    private val fileId: Long,
    private val dataViewModel: DataViewModel,
    private val enableHtml: Boolean
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FlashcardsListViewModel::class.java)){
            return FlashcardsListViewModel(database, fileId, dataViewModel, enableHtml) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}