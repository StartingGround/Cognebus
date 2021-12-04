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
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.startingground.cognebus.R
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.databinding.FragmentFlashcardAnswerBinding

class AnswerFragment : Fragment(), IntentInterface, InputToolbarInterface {

    private lateinit var binding: FragmentFlashcardAnswerBinding
    private lateinit var sharedFlashcardViewModel: FlashcardViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_flashcard_answer, container, false)

        val application = requireNotNull(this.activity).application
        val database = CognebusDatabase.getInstance(application)

        val flashcardViewModelFactory = FlashcardViewModelFactory(database, 0L, null)
        val temporarySharedFlashcardViewModel: FlashcardViewModel by navGraphViewModels(R.id.nav_file){flashcardViewModelFactory}
        sharedFlashcardViewModel = temporarySharedFlashcardViewModel

        requireActivity().onBackPressedDispatcher.addCallback(this){
            sharedFlashcardViewModel.clearDataOnLeavingWithoutAddingFlashcard()
            isEnabled = false
            activity?.onBackPressed()
        }

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
                R.id.save_flashcard -> {
                    onSaveFlashcardButton()
                    true
                }
                else -> false
            }
        }

        sharedFlashcardViewModel.answerError.observe(viewLifecycleOwner){
            binding.answerTextField.error = it
        }

        sharedFlashcardViewModel.questionError.observe(viewLifecycleOwner){
            if(it != null)  viewPager?.currentItem = 0
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
    }


    override val getImageFromCamera = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if(it) {
            placeImageTagInsideAnswerText()
        }
    }


    override val getImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if(uri != null) {
            val imageIsSaved = sharedFlashcardViewModel.saveImageToFileFromGalleryImageUri(uri)
            if(!imageIsSaved) return@registerForActivityResult
            placeImageTagInsideAnswerText()
        }
    }

    
    private fun placeImageTagInsideAnswerText(){
        val imageId = sharedFlashcardViewModel.getAddedImageId()
        val imageTagText = getString(R.string.image_html_tag_template, imageId)
        val (textCursorPositionStart, textCursorPositionEnd) = sharedFlashcardViewModel.getTextCursorPositions()
        binding.answerTextField.editText?.text?.replace(textCursorPositionStart, textCursorPositionEnd, imageTagText)
        sharedFlashcardViewModel.addAnswerText(binding.answerTextField.editText?.text?.toString() ?: "")
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
        sharedFlashcardViewModel.getImageFromCamera(this)
    }

    override fun onGetImageFromGalleryButton() {
        saveAnswersCursorPositions()
        sharedFlashcardViewModel.getImageFromGallery(this)
    }

    private fun saveAnswersCursorPositions(){
        val textCursorPositionStart = binding.answerTextField.editText?.selectionStart ?: 0
        val textCursorPositionEnd = binding.answerTextField.editText?.selectionEnd ?: 0
        sharedFlashcardViewModel.saveTextCursorPositions(textCursorPositionStart, textCursorPositionEnd)
    }


    private fun onSaveFlashcardButton(){
        val flashcardIsSaved = sharedFlashcardViewModel.saveFlashcardToDatabase()
        if(flashcardIsSaved){
            findNavController().popBackStack()
        }
    }
}