package com.startingground.cognebus.createorrenamefolderorfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class CreateOrRenameViewModelFactory(
    private val assistedFactory: CreateOrRenameViewModelAssistedFactory,
    private val folderId: Long?,
    private val inputType: Int,
    private val existingItemId: Long?
) : ViewModelProvider.Factory {
    override fun<T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CreateOrRenameViewModel::class.java)) {
            return assistedFactory.create(folderId, inputType, existingItemId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@AssistedFactory
interface CreateOrRenameViewModelAssistedFactory{
    fun create(
        @Assisted("folderId") folderId: Long?,
        @Assisted inputType: Int,
        @Assisted("existingFolderOrFileId") existingFolderOrFileId: Long?
    ): CreateOrRenameViewModel
}