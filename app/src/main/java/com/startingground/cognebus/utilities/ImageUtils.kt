package com.startingground.cognebus.utilities

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

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

    fun saveBitmapToFile(bitmap: Bitmap, file: File){
        val fileOutputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

        fileOutputStream.flush()
        fileOutputStream.close()
    }
}