package com.startingground.cognebus.practice

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class PracticePagerAdapter(
    fragment: Fragment,
    private val fragmentList: ArrayList<Fragment>
): FragmentStateAdapter(fragment){
    override fun getItemCount() = fragmentList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }
}