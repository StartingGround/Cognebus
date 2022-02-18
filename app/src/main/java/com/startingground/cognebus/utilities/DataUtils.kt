package com.startingground.cognebus.utilities

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.preference.PreferenceManager
import com.startingground.cognebus.database.CognebusDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataUtils @Inject constructor(
    @ApplicationContext applicationContext: Context,
    private val fileCognebusUtils: FileCognebusUtils,
    imageUtils: ImageUtils,
    database: CognebusDatabase,
) : DatabaseCognebusUtils(applicationContext, imageUtils, database){

    companion object{
        const val SHOW_DOLLAR_SIGN_ALERT = "dollar_sign_alert"
    }


    fun copyFileFromUri(uri: Uri, destinationFile: File) {
        scope?.launch {
            fileCognebusUtils.copyFileFromUri(uri, destinationFile)
        }
    }


    fun saveImageBitmapToFileWithImageId(imageBitmap: Bitmap, imageId: Long){
        scope?.launch {
            val imageFile = fileCognebusUtils.createFileOrGetExisting("images", "$imageId.jpg")
                ?: throw Exception("Could not get file from imageId")
            imageUtils.saveBitmapToFile(imageBitmap, imageFile)
        }
    }


    fun getStringFromResources(stringResource: Int): String{
        return appContext.getString(stringResource)
    }


    private val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)


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