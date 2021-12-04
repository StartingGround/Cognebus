package com.startingground.cognebus.flashcard

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

interface IntentInterface {
    val getImageFromCamera: ActivityResultLauncher<Uri>
    val getImageFromGallery: ActivityResultLauncher<String>
}