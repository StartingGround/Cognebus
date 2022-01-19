package com.startingground.cognebus.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory

@Suppress("UNCHECKED_CAST")
class FileViewModelFactory(
    private val assistedFactory: FileViewModelAssistedFactory,
    private val fileId: Long,
    private val dataViewModel: DataViewModel
    ) : ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(FileViewModel::class.java)){
            return assistedFactory.create(fileId, dataViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@AssistedFactory
interface FileViewModelAssistedFactory{
    fun create(
        @Assisted fileId: Long,
        @Assisted dataViewModel: DataViewModel
    ) : FileViewModel
}