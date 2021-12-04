package com.startingground.cognebus.directories

import androidx.recyclerview.selection.ItemKeyProvider

class DirectoriesItemKeyProvider(private val adapter: DirectoriesAdapter) :  ItemKeyProvider<String>(SCOPE_CACHED){
    override fun getKey(position: Int): String = adapter.getItemPublic(position).itemId

    override fun getPosition(key: String): Int = adapter.getPosition(key)
}