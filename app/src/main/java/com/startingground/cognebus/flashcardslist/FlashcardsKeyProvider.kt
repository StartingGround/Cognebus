package com.startingground.cognebus.flashcardslist

import androidx.recyclerview.selection.ItemKeyProvider

class FlashcardsKeyProvider(private val adapter: FlashcardsListAdapter) : ItemKeyProvider<Long>(SCOPE_CACHED) {
        override fun getKey(position: Int): Long = adapter.getItemId(position)

        override fun getPosition(key: Long): Int = adapter.getPosition(key)
}