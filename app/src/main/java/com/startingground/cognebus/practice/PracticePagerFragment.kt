package com.startingground.cognebus.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.startingground.cognebus.R

open class PracticePagerFragment : Fragment() {

    private val fragmentList = arrayListOf(
            QuestionPracticeFragment(),
            AnswerPracticeFragment()
        )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_practice_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val practicePagerAdapter = PracticePagerAdapter(this, fragmentList)
        val practiceViewPager2: ViewPager2 = view.findViewById(R.id.practice_pager)
        practiceViewPager2.adapter = practicePagerAdapter
    }
}