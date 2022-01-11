package com.startingground.cognebus.createorrenamefolderorfile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class CreateOrRenameViewModelFactory(
    private val database: CognebusDatabase?,
    private val folderId: Long?,
    private val inputType: Int,
    private val existingItemId: Long?,
    private val dataViewModel: DataViewModel,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun<T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CreateOrRenameViewModel::class.java)) {
            return CreateOrRenameViewModel(database, folderId, inputType, existingItemId, dataViewModel, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}