package com.startingground.cognebus.createorrenamefolderorfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class CreateOrRenameViewModelFactory(
    private val assistedFactory: CreateOrRenameViewModelAssistedFactory,
    private val folderId: Long?,
    private val inputType: Int,
    private val existingItemId: Long?,
    private val dataViewModel: DataViewModel
) : ViewModelProvider.Factory {
    override fun<T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CreateOrRenameViewModel::class.java)) {
            return assistedFactory.create(folderId, inputType, existingItemId, dataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@AssistedFactory
interface CreateOrRenameViewModelAssistedFactory{
    fun create(
        @Assisted("folderId") folderId: Long?,
        @Assisted inputType: Int,
        @Assisted("existingFolderOrFileId") existingFolderOrFileId: Long?,
        @Assisted dataViewModel: DataViewModel
    ): CreateOrRenameViewModel
}