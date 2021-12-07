package com.startingground.cognebus.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.startingground.cognebus.database.CognebusDatabase

@Suppress("UNCHECKED_CAST")
class MainMenuViewModelFactory(private val database: CognebusDatabase): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MainMenuViewModel::class.java)){
            return MainMenuViewModel(database) as T
        }

        throw IllegalArgumentException("Unknown view model class!")
    }
}