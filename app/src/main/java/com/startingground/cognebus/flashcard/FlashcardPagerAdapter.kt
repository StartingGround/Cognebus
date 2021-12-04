package com.startingground.cognebus.flashcard

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class FlashcardPagerAdapter(
    fragment: Fragment,
    private val fragmentList: ArrayList<Fragment>
): FragmentStateAdapter(fragment) {

    override fun getItemCount() = fragmentList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }
}