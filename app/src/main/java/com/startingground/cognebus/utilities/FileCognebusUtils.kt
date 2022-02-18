package com.startingground.cognebus.utilities

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class FileCognebusUtils @Inject constructor(@ApplicationContext private val appContext: Context) {
    suspend fun copyFileFromUri(uri: Uri, destinationFile: File) {
        withContext(Dispatchers.IO) {
            val uriFileDescriptor = appContext.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
            val uriFileChannel = FileInputStream(uriFileDescriptor).channel

            val destinationFileChannel = FileOutputStream(destinationFile).channel

            destinationFileChannel.transferFrom(uriFileChannel, 0, uriFileChannel.size())

            uriFileChannel.close()
            destinationFileChannel.close()
        }
    }


    fun createFileOrGetExisting(directory: String, fileName: String): File?{
        return try {
            val fileDirectory = appContext.getExternalFilesDir(directory)
            val file = File(fileDirectory, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            file
        } catch(e: Exception) {
            null
        }
    }


    fun getFileExtensionFromExternalUri(uri: Uri): String?{
        val mimeType = appContext.contentResolver.getType(uri)
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(mimeType)
    }
}