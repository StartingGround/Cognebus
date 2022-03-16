package com.startingground.cognebus.flashcard

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
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
import com.startingground.cognebus.databinding.FragmentFlashcardAnswerBinding
import com.startingground.cognebus.settings.SettingsViewModel
import com.startingground.cognebus.utilities.OnTouchListenerForScrollingInsideViewPager2
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AnswerFragment : Fragment(), InputToolbarInterface {

    private lateinit var binding: FragmentFlashcardAnswerBinding
    @Inject lateinit var sharedFlashcardViewModelAssistedFactory: FlashcardViewModelAssistedFactory
    private lateinit var sharedFlashcardViewModel: FlashcardViewModel

    private var consecutiveFlashcardCreationIsEnabled: Boolean = SettingsViewModel.CONSECUTIVE_FLASHCARD_CREATION_DEFAULT_VALUE
    private var cropImageWhenAdded: Boolean = SettingsViewModel.CROP_IMAGE_WHEN_ADDED_DEFAULT_VALUE


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_flashcard_answer, container, false)

        val flashcardViewModelFactory = FlashcardViewModelFactory(sharedFlashcardViewModelAssistedFactory, 0L)
        val temporarySharedFlashcardViewModel: FlashcardViewModel by navGraphViewModels(R.id.nav_file){flashcardViewModelFactory}
        sharedFlashcardViewModel = temporarySharedFlashcardViewModel

        requireActivity().onBackPressedDispatcher.addCallback(this){
            sharedFlashcardViewModel.clearDataOnLeavingWithoutAddingFlashcard()
            isEnabled = false
            activity?.onBackPressed()
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        consecutiveFlashcardCreationIsEnabled = preferences.getBoolean(
            SettingsViewModel.CONSECUTIVE_FLASHCARD_CREATION_KEY,
            SettingsViewModel.CONSECUTIVE_FLASHCARD_CREATION_DEFAULT_VALUE
        )

        cropImageWhenAdded = preferences.getBoolean(
            SettingsViewModel.CROP_IMAGE_WHEN_ADDED_KEY,
            SettingsViewModel.CROP_IMAGE_WHEN_ADDED_DEFAULT_VALUE
        )

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.answerFragment = this
        binding.sharedFlashcardViewModel = sharedFlashcardViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val viewPager: ViewPager2? = activity?.findViewById(R.id.flashcard_pager)

        binding.topAppBar.setNavigationOnClickListener {
            viewPager?.currentItem = 0
        }

        binding.topAppBar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.preview -> {
                    sharedFlashcardViewModel.onPreviewButton()
                    true
                }
                R.id.save_flashcard -> {
                    onSaveFlashcardButton()
                    true
                }
                else -> false
            }
        }

        sharedFlashcardViewModel.previewModeIsEnabled.observe(viewLifecycleOwner){
            changeViewsOnPreviewButton(it)
        }

        sharedFlashcardViewModel.answerError.observe(viewLifecycleOwner){
            binding.answerTextField.error = it
        }

        sharedFlashcardViewModel.questionError.observe(viewLifecycleOwner){
            if(it != null)  viewPager?.currentItem = 0
        }

        sharedFlashcardViewModel.getImageFromGalleryTrigger.observe(viewLifecycleOwner){
            if(it != IntentCaller.ANSWER) return@observe
            sharedFlashcardViewModel.clearIntentTriggers()
            getImageFromGallery.launch("image/*")
        }

        sharedFlashcardViewModel.getImageFromCameraTrigger.observe(viewLifecycleOwner){
            val caller = it.first
            val uri = it.second
            if(caller != IntentCaller.ANSWER || uri == null) return@observe
            sharedFlashcardViewModel.clearIntentTriggers()
            getImageFromCamera.launch(uri)
        }

        binding.answerTextField.editText?.doOnTextChanged { text, _, _, _ ->
            sharedFlashcardViewModel.addAnswerText(text.toString())

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

        binding.answerTextField.editText?.setOnTouchListener(
            OnTouchListenerForScrollingInsideViewPager2(binding.answerTextField.editText, requireContext())
        )

        binding.answerMathView.setOnTouchListener(
            OnTouchListenerForScrollingInsideViewPager2(binding.answerMathView, requireContext())
        )
    }


    private fun changeViewsOnPreviewButton(previewIsEnabled: Boolean){
        if(previewIsEnabled){
            binding.topAppBar.menu.findItem(R.id.preview).setIcon(R.drawable.ic_visibility_off_24)
            binding.topAppBar.menu.findItem(R.id.preview).setTitle(R.string.flashcard_top_app_bar_disable_preview)
            binding.topAppBar.menu.findItem(R.id.preview).contentDescription =
                getString(R.string.flashcard_top_app_bar_disable_preview_content_description)

            binding.answerTextField.visibility = View.GONE
            binding.answerMathView.visibility = View.VISIBLE

            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(binding.answerTextField.editText?.windowToken, 0)
        } else{
            binding.topAppBar.menu.findItem(R.id.preview).setIcon(R.drawable.ic_visibility_24)
            binding.topAppBar.menu.findItem(R.id.preview).setTitle(R.string.flashcard_top_app_bar_preview)
            binding.topAppBar.menu.findItem(R.id.preview).contentDescription =
                getString(R.string.flashcard_top_app_bar_preview_content_description)

            binding.answerTextField.visibility = View.VISIBLE
            binding.answerMathView.visibility = View.GONE
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
        binding.answerTextField.editText?.text?.replace(textCursorPositionStart, textCursorPositionEnd, imageTagText)
        sharedFlashcardViewModel.addAnswerText(binding.answerTextField.editText?.text?.toString() ?: "")
    }


    private fun openImageForCropping(){
        val image = sharedFlashcardViewModel.getAddedImage()
        if(image.fileExtension == "gif") return
        val action = FlashcardPagerFragmentDirections.actionFlashcardPagerFragmentToImageCropFragment(image.imageId, image.fileExtension)
        findNavController().navigate(action)
    }


    fun onAnswerTextFieldClicked(){
        binding.answerTextField.editText?.requestFocus()
        val position = binding.answerTextField.editText?.text?.length ?: 0
        binding.answerTextField.editText?.setSelection(position)
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.answerTextField.editText, InputMethodManager.SHOW_IMPLICIT)
    }


    override fun onGetImageFromCameraButton() {
        saveAnswersCursorPositions()
        sharedFlashcardViewModel.getImageFromCamera(IntentCaller.ANSWER)
    }

    override fun onGetImageFromGalleryButton() {
        saveAnswersCursorPositions()
        sharedFlashcardViewModel.getImageFromGallery(IntentCaller.ANSWER)
    }

    private fun saveAnswersCursorPositions(){
        val textCursorPositionStart = binding.answerTextField.editText?.selectionStart ?: 0
        val textCursorPositionEnd = binding.answerTextField.editText?.selectionEnd ?: 0
        sharedFlashcardViewModel.saveTextCursorPositions(textCursorPositionStart, textCursorPositionEnd)
    }


    private fun onSaveFlashcardButton(){
        val thereIsExistingFlashcard: Boolean = sharedFlashcardViewModel.flashcard?.thereIsExistingFlashcard() ?: false
        val flashcardIsSaved = sharedFlashcardViewModel.saveFlashcardToDatabase()
        if(!flashcardIsSaved) return

        Toast.makeText(context, R.string.flashcard_saved_toast, Toast.LENGTH_SHORT).show()

        if(consecutiveFlashcardCreationIsEnabled && !thereIsExistingFlashcard){
            val action = FlashcardPagerFragmentDirections.actionFlashcardPagerFragmentSelf(sharedFlashcardViewModel.fileId, 0L)
            findNavController().navigate(action)
            return
        }

        findNavController().popBackStack()
    }
}