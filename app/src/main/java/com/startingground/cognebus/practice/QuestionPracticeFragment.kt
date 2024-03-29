package com.startingground.cognebus.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentQuestionPracticeBinding
import com.startingground.cognebus.utilities.OnTouchListenerForScrollingInsideViewPager2
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuestionPracticeFragment : Fragment() {

    private var binding: FragmentQuestionPracticeBinding? = null
    private val sharedPracticeViewModel: PracticeViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_question_practice, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.sharedPracticeViewModel = sharedPracticeViewModel
        binding?.lifecycleOwner = viewLifecycleOwner

        sharedPracticeViewModel.currentFlashcard.observe(viewLifecycleOwner){
            if(it == null) {
                findNavController().popBackStack()
            }
        }

        binding?.topAppBar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        val viewPager: ViewPager2? =  activity?.findViewById(R.id.practice_pager)

        binding?.topAppBar?.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.move_to_answer ->{
                    viewPager?.currentItem = 1
                    true
                }
                else -> false
            }
        }

        binding?.topAppBar?.title = getString(R.string.practice_question_fragment_top_app_bar_title, 0, 0)

        sharedPracticeViewModel.flashcardNumber.observe(viewLifecycleOwner){
            val (currentFlashcardNumber, totalNumberOfFlashcards) = it
            binding?.topAppBar?.title = getString(R.string.practice_question_fragment_top_app_bar_title, currentFlashcardNumber, totalNumberOfFlashcards)
        }

        binding?.questionMathView?.setOnTouchListener(
            OnTouchListenerForScrollingInsideViewPager2(binding?.questionMathView, requireContext())
        )
    }
}