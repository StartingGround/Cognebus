package com.startingground.cognebus

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.canhub.cropper.CropImageView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CropImageFragment : Fragment(), CropImageView.OnCropImageCompleteListener {

    private lateinit var dataViewModel: DataViewModel

    private var imageId: Long? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            imageId = it.getLong("imageId")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val application = requireNotNull(this.activity).application

        val dataViewModelFactory = DataViewModelFactory(application)
        dataViewModel = ViewModelProvider(this.requireActivity(), dataViewModelFactory).get(DataViewModel::class.java)

        return inflater.inflate(R.layout.fragment_crop_image, container, false)
    }


    private lateinit var cropImageView: CropImageView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cropImageView = view.findViewById(R.id.crop_image_view)
        val imageFile = imageId?.let { dataViewModel.createImageFileOrGetExisting(it) }

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
                .setMessage(R.string.crop_image_dialog_image_not_cropped_message)
                .setPositiveButton(R.string.crop_image_dialog_image_not_cropped_positive_button){ _, _ ->}
                .show()

            return
        }

        val imageBitmap = cropImageView.croppedImage

        imageBitmap?.let { image ->
            imageId?.let { id ->
                dataViewModel.saveImageBitmapToUri(image, id)
            }
        }
        
        activity?.onBackPressed()
    }
}