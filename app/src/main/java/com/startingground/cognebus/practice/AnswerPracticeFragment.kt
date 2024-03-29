package com.startingground.cognebus.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.startingground.cognebus.R
import com.startingground.cognebus.databinding.FragmentAnswerPracticeBinding
import com.startingground.cognebus.utilities.OnTouchListenerForScrollingInsideViewPager2
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnswerPracticeFragment : Fragment() {

    private var binding: FragmentAnswerPracticeBinding? = null
    private val sharedPracticeViewModel: PracticeViewModel by activityViewModels()

    private var viewPager: ViewPager2? = null

    //If we navigate to this fragment, initial current flashcard will trigger observer and set viewpager to show question fragment
    private var currentFlashcardsFirstChange: Boolean = true


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_answer_practice, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.sharedPracticeViewModel = sharedPracticeViewModel
        binding?.lifecycleOwner = viewLifecycleOwner

        viewPager = activity?.findViewById(R.id.practice_pager)

        sharedPracticeViewModel.currentFlashcard.observe(viewLifecycleOwner){
            if(currentFlashcardsFirstChange){
                currentFlashcardsFirstChange = false
                return@observe
            }

            viewPager?.currentItem = 0
        }

        val viewPager: ViewPager2? =  activity?.findViewById(R.id.practice_pager)

        binding?.topAppBar?.setNavigationOnClickListener {
            viewPager?.currentItem = 0
        }

        binding?.answerMathView?.setOnTouchListener(
            OnTouchListenerForScrollingInsideViewPager2(binding?.answerMathView, requireContext())
        )
    }
}