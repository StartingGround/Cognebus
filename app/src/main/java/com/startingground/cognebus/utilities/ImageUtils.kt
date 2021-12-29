package com.startingground.cognebus.utilities

import android.content.Context
import java.io.File

object ImageUtils {
    fun deleteImageFileById(imageId: Long, context: Context){
        val imageFile = getImageFile(imageId, context) ?: return
        if(imageFile.exists()){
            imageFile.delete()
        }
    }


    fun getImageFile(imageId: Long, context: Context): File?{
        return try {
            val imageDirectory = context.getExternalFilesDir("images")
            File(imageDirectory, "$imageId.jpg")
        } catch(e: Exception) {
            null
        }
    }
}