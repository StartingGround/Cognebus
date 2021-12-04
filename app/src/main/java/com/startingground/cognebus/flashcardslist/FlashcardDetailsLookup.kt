package com.startingground.cognebus.flashcardslist

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView

class FlashcardDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
    override fun getItemDetails(e: MotionEvent): ItemDetails<Long> {
        val view = recyclerView.findChildViewUnder(e.x, e.y) ?: return EMPTY_FLASHCARD_ITEM

        return (recyclerView.getChildViewHolder(view) as FlashcardsListAdapter.FlashcardViewHolder).getItemDetails()
    }

    object EMPTY_FLASHCARD_ITEM : ItemDetails<Long>() {
        override fun getSelectionKey(): Long = 0
        override fun getPosition(): Int = Integer.MAX_VALUE
    }
}