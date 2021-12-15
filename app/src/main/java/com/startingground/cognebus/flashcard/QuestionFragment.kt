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
import com.startingground.cognebus.databinding.FragmentFlashcardQuestionBinding

class QuestionFragment : Fragment(), IntentInterface, InputToolbarInterface {

    private lateinit var binding: FragmentFlashcardQuestionBinding
    private lateinit var sharedFlashcardViewModel: FlashcardViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_flashcard_question, container, false)

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

        binding.questionFragment = this
        binding.sharedFlashcardViewModel = sharedFlashcardViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.topAppBar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        val viewPager: ViewPager2? =  activity?.findViewById(R.id.flashcard_pager)

        binding.topAppBar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.move_to_answer ->{
                    viewPager?.currentItem = 1
                    true
                }
                else -> false
            }
        }

        sharedFlashcardViewModel.questionError.observe(viewLifecycleOwner){
            binding.questionTextField.error = it
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


    override val getImageFromCamera = registerForActivityResult(ActivityResultContracts.TakePicture()){
        if(!it) return@registerForActivityResult

        placeImageTagInsideAnswerText()

        openImageForCropping()
    }


    override val getImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if(uri == null) return@registerForActivityResult

        val imageIsSaved = sharedFlashcardViewModel.saveImageToFileFromGalleryImageUri(uri)
        if (!imageIsSaved) return@registerForActivityResult
        placeImageTagInsideAnswerText()

        openImageForCropping()
    }


    private fun placeImageTagInsideAnswerText(){
        val imageId = sharedFlashcardViewModel.getAddedImageId()
        val imageTagText = getString(R.string.image_html_tag_template, imageId)
        val (textCursorPositionStart, textCursorPositionEnd) = sharedFlashcardViewModel.getTextCursorPositions()
        binding.questionTextField.editText?.text?.replace(textCursorPositionStart, textCursorPositionEnd, imageTagText)
        sharedFlashcardViewModel.addQuestionText(binding.questionTextField.editText?.text?.toString() ?: "")
    }


    private fun openImageForCropping(){
        val imageId = sharedFlashcardViewModel.getAddedImageId()
        val action = FlashcardPagerFragmentDirections.actionFlashcardPagerFragmentToImageCropFragment(imageId)
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
        sharedFlashcardViewModel.getImageFromCamera(this)
    }


    override fun onGetImageFromGalleryButton(){
        saveQuestionsCursorPositions()
        sharedFlashcardViewModel.getImageFromGallery(this)
    }


    private fun saveQuestionsCursorPositions(){
        val textCursorPositionStart = binding.questionTextField.editText?.selectionStart ?: 0
        val textCursorPositionEnd = binding.questionTextField.editText?.selectionEnd ?: 0
        sharedFlashcardViewModel.saveTextCursorPositions(textCursorPositionStart, textCursorPositionEnd)
    }
}