package com.startingground.cognebus.sharedviewmodels

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.startingground.cognebus.utilities.FileCognebusUtils
import com.startingground.cognebus.utilities.FlashcardUtils
import com.startingground.cognebus.utilities.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DataViewModel(app: Application) : DatabaseInputViewModel(app){

    companion object{
        const val SHOW_DOLLAR_SIGN_ALERT = "dollar_sign_alert"
    }

    fun copyFileFromUri(uri: Uri, destinationFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            FileCognebusUtils.copyFileFromUri(uri, destinationFile, context)
        }
    }

    fun saveImageBitmapToUri(imageBitmap: Bitmap, imageId: Long){
        viewModelScope.launch(Dispatchers.IO) {
            val imageFile = createImageFileOrGetExisting(imageId) ?: throw Exception("Could not get file from imageId")
            ImageUtils.saveBitmapToFile(imageBitmap, imageFile)
        }
    }

    fun createImageFileOrGetExisting(imageId: Long): File?{
        val context = getApplication<Application>().applicationContext
        return FileCognebusUtils.createFileOrGetExisting("images", "$imageId.jpg", context)
    }

    fun getStringFromResources(stringResource: Int): String{
        val context = getApplication<Application>().applicationContext
        return context.getString(stringResource)
    }

    fun prepareStringForPracticeCaller(inputText: String, enableHTML: Boolean): String{
        val context = getApplication<Application>().applicationContext
        return FlashcardUtils.prepareStringForPractice(context, inputText, enableHTML)
    }


    private val preferences = PreferenceManager.getDefaultSharedPreferences(app.applicationContext)


    fun getShowDollarSignAlert(): Boolean{
        return preferences.getBoolean(
            SHOW_DOLLAR_SIGN_ALERT, true
        )
    }


    fun setShowDollarSignAlert(state: Boolean){
        with(preferences.edit()){
            putBoolean(SHOW_DOLLAR_SIGN_ALERT, state)
            apply()
        }
    }
}