package com.startingground.cognebus.directories

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView

class DirectoriesItemDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<String>(){
    override fun getItemDetails(e: MotionEvent): ItemDetails<String> {
        val view = recyclerView.findChildViewUnder(e.x, e.y) ?: return EMPTY_DIRECTORIES_ITEM

        return (recyclerView.getChildViewHolder(view) as DirectoriesAdapter.ItemViewHolder).getItemDetails()
    }

    object EMPTY_DIRECTORIES_ITEM : ItemDetails<String>() {
        const val NONE = "none"
        override fun getSelectionKey(): String = NONE
        override fun getPosition(): Int = Integer.MAX_VALUE
    }
}