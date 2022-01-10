package com.startingground.cognebus.utilities

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileCognebusUtils {
    fun copyFileFromUri(uri: Uri, destinationFile: File, context: Context) {
        val uriFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
        val uriFileChannel = FileInputStream(uriFileDescriptor).channel

        val destinationFileChannel = FileOutputStream(destinationFile).channel

        destinationFileChannel.transferFrom(uriFileChannel, 0, uriFileChannel.size())

        uriFileChannel.close()
        destinationFileChannel.close()
    }


    fun createFileOrGetExisting(directory: String, fileName: String, context: Context): File?{
        return try {
            val fileDirectory = context.getExternalFilesDir(directory)
            val file = File(fileDirectory, fileName)
            if(!file.exists()){
                file.createNewFile()
            }
            file
        } catch(e: Exception) {
            null
        }
    }


    fun getFileExtensionFromExternalUri(uri: Uri, context: Context): String?{
        val mimeType = context.contentResolver.getType(uri)
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(mimeType)
    }
}