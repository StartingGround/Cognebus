package com.startingground.cognebus.createfolderorfile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class CreateViewModelFactory(
    private val database: CognebusDatabase?,
    private val folderId: Long?,
    private val inputType: Int,
    private val dataViewModel: DataViewModel,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun<T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CreateViewModel::class.java)) {
            return CreateViewModel(database, folderId, inputType, dataViewModel, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}