package com.startingground.cognebus.flashcard

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentFlashcardQuestionBinding
import com.startingground.cognebus.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class QuestionFragment : Fragment(), InputToolbarInterface {

    private lateinit var binding: FragmentFlashcardQuestionBinding
    @Inject lateinit var sharedFlashcardViewModelAssistedFactory: FlashcardViewModelAssistedFactory
    private lateinit var sharedFlashcardViewModel: FlashcardViewModel

    private var cropImageWhenAdded: Boolean = SettingsViewModel.CROP_IMAGE_WHEN_ADDED_DEFAULT_VALUE


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_flashcard_question, container, false)

        val application = requireNotNull(this.activity).application

        val flashcardViewModelFactory = FlashcardViewModelFactory(sharedFlashcardViewModelAssistedFactory, 0L, null, application)
        val temporarySharedFlashcardViewModel: FlashcardViewModel by navGraphViewModels(R.id.nav_file){flashcardViewModelFactory}
        sharedFlashcardViewModel = temporarySharedFlashcardViewModel

        requireActivity().onBackPressedDispatcher.addCallback(this){
            sharedFlashcardViewModel.clearDataOnLeavingWithoutAddingFlashcard()
            isEnabled = false
            activity?.onBackPressed()
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        cropImageWhenAdded = preferences.getBoolean(
            SettingsViewModel.CROP_IMAGE_WHEN_ADDED_KEY,
            SettingsViewModel.CROP_IMAGE_WHEN_ADDED_DEFAULT_VALUE
        )

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.questionFragment = this
        binding.sharedFlashcardViewModel = sharedFlashcardViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.topAppBar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        val viewPager: ViewPager2? =  activity?.findViewById(R.id.flashcard_pager)

        binding.topAppBar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.preview -> {
                    sharedFlashcardViewModel.onPreviewButton()
                    true
                }
                R.id.move_to_answer -> {
                    viewPager?.currentItem = 1
                    true
                }
                else -> false
            }
        }

        sharedFlashcardViewModel.questionError.observe(viewLifecycleOwner){
            binding.questionTextField.error = it
        }

        sharedFlashcardViewModel.previewModeIsEnabled.observe(viewLifecycleOwner){
            changeViewsOnPreviewButton(it)
        }

        sharedFlashcardViewModel.getImageFromGalleryTrigger.observe(viewLifecycleOwner){
            if(it != IntentCaller.QUESTION) return@observe
            sharedFlashcardViewModel.clearIntentTriggers()
            getImageFromGallery.launch("image/*")
        }

        sharedFlashcardViewModel.getImageFromCameraTrigger.observe(viewLifecycleOwner){
            val caller = it.first
            val uri = it.second
            if(caller != IntentCaller.QUESTION || uri == null) return@observe
            sharedFlashcardViewModel.clearIntentTriggers()
            getImageFromCamera.launch(uri)
        }

        binding.questionTextField.editText?.doOnTextChanged { text, _, _, _ ->
            sharedFlashcardViewModel.addQuestionText(text.toString())

            if(text?.lastOrNull() == '$' && sharedFlashcardViewModel.showDollarSignAlert){
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dollar_sign_alert_title)
                    .setPositiveButton(R.string.dollar_sign_alert_ok) { _, _ -> }
                    .setNeutralButton(R.string.dollar_sign_alert_dont_show_again) { _, _ ->
                        sharedFlashcardViewModel.disableDollarSignAlert()
                    }
                    .show()
            }
        }
    }


    private fun changeViewsOnPreviewButton(previewIsEnabled: Boolean){
        if(previewIsEnabled){
            binding.topAppBar.menu.findItem(R.id.preview).setIcon(R.drawable.ic_visibility_off_24)
            binding.topAppBar.menu.findItem(R.id.preview).setTitle(R.string.flashcard_top_app_bar_disable_preview)
            binding.topAppBar.menu.findItem(R.id.preview).contentDescription =
                getString(R.string.flashcard_top_app_bar_disable_preview_content_description)

            binding.questionNestedScrollView.visibility = View.GONE
            binding.questionMathViewNestedScrollView.visibility = View.VISIBLE

            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.questionTextField.editText?.windowToken, 0)
        } else{
            binding.topAppBar.menu.findItem(R.id.preview).setIcon(R.drawable.ic_visibility_24)
            binding.topAppBar.menu.findItem(R.id.preview).setTitle(R.string.flashcard_top_app_bar_preview)
            binding.topAppBar.menu.findItem(R.id.preview).contentDescription =
                getString(R.string.flashcard_top_app_bar_preview_content_description)

            binding.questionNestedScrollView.visibility = View.VISIBLE
            binding.questionMathViewNestedScrollView.visibility = View.GONE
        }
    }


    private val getImageFromCamera = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if(!it) return@registerForActivityResult

        placeImageTagInsideAnswerText()

        if(cropImageWhenAdded) openImageForCropping()
    }


    private val getImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if(uri == null) return@registerForActivityResult

        val imageIsSaved = sharedFlashcardViewModel.saveImageToFileFromGalleryImageUri(uri)
        if (!imageIsSaved) return@registerForActivityResult
        placeImageTagInsideAnswerText()

        if(cropImageWhenAdded) openImageForCropping()
    }


    private fun placeImageTagInsideAnswerText(){
        val image = sharedFlashcardViewModel.getAddedImage()
        val imageTagText = getString(R.string.image_html_tag_template, image.imageId)
        val (textCursorPositionStart, textCursorPositionEnd) = sharedFlashcardViewModel.getTextCursorPositions()
        binding.questionTextField.editText?.text?.replace(textCursorPositionStart, textCursorPositionEnd, imageTagText)
        sharedFlashcardViewModel.addQuestionText(binding.questionTextField.editText?.text?.toString() ?: "")
    }


    private fun openImageForCropping(){
        val image = sharedFlashcardViewModel.getAddedImage()
        if(image.fileExtension == "gif") return
        val action = FlashcardPagerFragmentDirections.actionFlashcardPagerFragmentToImageCropFragment(image.imageId, image.fileExtension)
        findNavController().navigate(action)
    }


    fun onQuestionTextFieldClicked(){
        binding.questionTextField.editText?.requestFocus()
        val position = binding.questionTextField.editText?.text?.length ?: 0
        binding.questionTextField.editText?.setSelection(position)
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.questionTextField.editText, InputMethodManager.SHOW_IMPLICIT)
    }


    override fun onGetImageFromCameraButton(){
        saveQuestionsCursorPositions()
        sharedFlashcardViewModel.getImageFromCamera(IntentCaller.QUESTION)
    }


    override fun onGetImageFromGalleryButton(){
        saveQuestionsCursorPositions()
        sharedFlashcardViewModel.getImageFromGallery(IntentCaller.QUESTION)
    }


    private fun saveQuestionsCursorPositions(){
        val textCursorPositionStart = binding.questionTextField.editText?.selectionStart ?: 0
        val textCursorPositionEnd = binding.questionTextField.editText?.selectionEnd ?: 0
        sharedFlashcardViewModel.saveTextCursorPositions(textCursorPositionStart, textCursorPositionEnd)
    }
}