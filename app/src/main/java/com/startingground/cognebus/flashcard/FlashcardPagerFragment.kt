package com.startingground.cognebus.flashcard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentFlashcardPagerBinding
import dagger.hilt.android.AndroidEntryPoint
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import javax.inject.Inject

@AndroidEntryPoint
class FlashcardPagerFragment : Fragment() {

    @Inject lateinit var sharedFlashcardViewModelAssistedFactory: FlashcardViewModelAssistedFactory

    private var fileId: Long? = null
    private var flashcardId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            fileId = it.getLong("fileId")
            if(fileId == 0L){
                fileId = null
            }

            flashcardId = it.getLong("flashcardId")
            if(flashcardId == 0L){
                flashcardId = null
            }
        }
    }


    private lateinit var binding: FragmentFlashcardPagerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val application = requireNotNull(this.activity).application

        val flashcardViewModelFactory = FlashcardViewModelFactory(
            sharedFlashcardViewModelAssistedFactory,
            fileId ?: throw IllegalArgumentException("fileId can't be null"),
            application
        )
        val sharedFlashcardViewModel: FlashcardViewModel by navGraphViewModels(R.id.nav_file){flashcardViewModelFactory}

        if (sharedFlashcardViewModel.flashcard == null) {
            if (flashcardId == null) {
                sharedFlashcardViewModel.createNewFlashcard()
            } else {
                sharedFlashcardViewModel.setExistingFlashcard(flashcardId!!)
            }
        }

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_flashcard_pager, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentList = arrayListOf<Fragment>(
            QuestionFragment(),
            AnswerFragment()
        )

        val flashcardPagerAdapter = FlashcardPagerAdapter(this, fragmentList)
        binding.flashcardPager.adapter = flashcardPagerAdapter

        binding.flashcardPagerFragment = this

        KeyboardVisibilityEvent.setEventListener(requireActivity(), object: KeyboardVisibilityEventListener {
            override fun onVisibilityChanged(isOpen: Boolean) {
                binding.inputToolbar.visibility = if(isOpen) View.VISIBLE else View.GONE
            }
        })
    }


    fun onGetImageFromCameraButton(){
        val currentFragment = getCurrentFragmentAsInputToolbarInterface()
        currentFragment?.onGetImageFromCameraButton()
    }


    fun onGetImageFromGalleryButton(){
        val currentFragment = getCurrentFragmentAsInputToolbarInterface()
        currentFragment?.onGetImageFromGalleryButton()
    }


    private fun getCurrentFragmentAsInputToolbarInterface(): InputToolbarInterface?{
        val fragmentPosition = binding.flashcardPager.currentItem
        val fragment = childFragmentManager.findFragmentByTag("f$fragmentPosition")

        return if(fragment is InputToolbarInterface) fragment else null
    }
}