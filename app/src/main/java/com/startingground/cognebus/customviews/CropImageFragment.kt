package com.startingground.cognebus.customviews

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.canhub.cropper.CropImageView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.startingground.cognebus.utilities.DataUtils
import com.startingground.cognebus.R
import com.startingground.cognebus.flashcard.FlashcardViewModel
import com.startingground.cognebus.flashcard.FlashcardViewModelAssistedFactory
import com.startingground.cognebus.flashcard.FlashcardViewModelFactory
import com.startingground.cognebus.utilities.FileCognebusUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CropImageFragment : Fragment(), CropImageView.OnCropImageCompleteListener {

    @Inject lateinit var dataUtils: DataUtils
    @Inject lateinit var fileCognebusUtils: FileCognebusUtils
    @Inject lateinit var sharedFlashcardViewModelAssistedFactory: FlashcardViewModelAssistedFactory
    private lateinit var sharedFlashcardViewModel: FlashcardViewModel

    private var imageId: Long? = null
    private var fileExtension: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var fileExtensionTemp: String? = null
        arguments?.let{
            imageId = it.getLong("imageId")
            fileExtensionTemp = it.getString("fileExtension")
        }

        if(fileExtensionTemp == null) findNavController().popBackStack()
        fileExtension = fileExtensionTemp!!
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val flashcardViewModelFactory = FlashcardViewModelFactory(sharedFlashcardViewModelAssistedFactory, 0L)
        val temporarySharedFlashcardViewModel: FlashcardViewModel by navGraphViewModels(R.id.nav_file){flashcardViewModelFactory}
        sharedFlashcardViewModel = temporarySharedFlashcardViewModel

        return inflater.inflate(R.layout.fragment_crop_image, container, false)
    }


    private lateinit var cropImageView: CropImageView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cropImageView = view.findViewById(R.id.crop_image_view)
        val imageFile = imageId?.let {
            fileCognebusUtils.createFileOrGetExisting("images", "$it.$fileExtension")
        }

        if(imageFile == null) activity?.onBackPressed()

        cropImageView.setImageUriAsync(imageFile?.toUri())
        cropImageView.setOnCropImageCompleteListener(this)

        val topAppBar: MaterialToolbar = view.findViewById(R.id.top_app_bar)

        topAppBar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId){
                R.id.rotate_image -> {
                    cropImageView.rotateImage(90)
                    true
                }
                R.id.crop_image -> {
                    cropImageView.croppedImageAsync()
                    true
                }
                else -> false
            }
        }
    }


    override fun onDetach() {
        super.onDetach()
        cropImageView.setOnCropImageCompleteListener(null)
    }


    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error != null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.crop_image_dialog_image_not_cropped_message)
                .setPositiveButton(R.string.crop_image_dialog_image_not_cropped_positive_button){ _, _ ->}
                .show()

            return
        }

        val imageBitmap = cropImageView.croppedImage

        imageBitmap?.let { image ->
            imageId?.let { id ->
                dataUtils.saveImageBitmapToFileWithImageId(image, id)

                sharedFlashcardViewModel.flashcard?.changeImageFileExtension(id, "jpg")
            }
        }
        
        activity?.onBackPressed()
    }
}