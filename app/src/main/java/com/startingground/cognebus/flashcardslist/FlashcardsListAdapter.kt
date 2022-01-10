package com.startingground.cognebus.flashcardslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.startingground.cognebus.R
import com.startingground.cognebus.customviews.MathView
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.flashcardslist.FlashcardsListAdapter.FlashcardViewHolder

class FlashcardsListAdapter : ListAdapter<FlashcardAdapterItem, FlashcardViewHolder>(FlashcardsListDiffCallback()  ) {

    init {
        setHasStableIds(true)
    }

    var selectionTracker: SelectionTracker<Long>? = null


    private val _flashcardClicked: MutableLiveData<FlashcardDB?> = MutableLiveData(null)
    val flashcardClicked: LiveData<FlashcardDB?> get() = _flashcardClicked

    fun clearFlashcardClickTrigger(){
        _flashcardClicked.value = null
    }

    fun onFlashcardClicked(flashcardDB: FlashcardDB?){
        if(flashcardDB == null) return
        _flashcardClicked.value = flashcardDB
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlashcardViewHolder {
        return FlashcardViewHolder.create(parent, this)
    }


    override fun onBindViewHolder(holder: FlashcardViewHolder, position: Int) {
        val flashcardAdapterItem = getItem(position)
        holder.bind(flashcardAdapterItem)
        selectionTracker?.let {
            val selected: Boolean = it.isSelected(getItemId(position))
            holder.selected(selected)
        }
    }


    class FlashcardViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val questionMathView: MathView = view.findViewById(R.id.question_math_view_item)
        private val materialCardView: MaterialCardView = view.findViewById(R.id.flashcard_card_view_item)
        var flashcard: FlashcardDB? = null

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>(){
                override fun getPosition(): Int = adapterPosition

                override fun getSelectionKey(): Long = itemId
            }

        fun selected(selected: Boolean){
            materialCardView.isChecked = selected
        }

        fun bind(flashcardItem: FlashcardAdapterItem){
            questionMathView.text = flashcardItem.questionText

            val foregroundColor = if(flashcardItem.flashcard.repetitionEnabled){
                ContextCompat.getColorStateList(materialCardView.context, R.color.flashcard_item_foreground_default)
            } else{
                ContextCompat.getColorStateList(materialCardView.context, R.color.flashcard_item_foreground_disabled)
            }

            materialCardView.setCardForegroundColor(foregroundColor)

            flashcard = flashcardItem.flashcard
        }

        companion object{
            fun create(parent: ViewGroup, adapter: FlashcardsListAdapter) : FlashcardViewHolder{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.flashcard_item, parent, false)
                val flashcardViewHolder = FlashcardViewHolder(view)

                flashcardViewHolder.questionMathView.settings.allowFileAccess = true
                flashcardViewHolder.questionMathView.setCustomClickHandler {
                    adapter.onFlashcardClicked(flashcardViewHolder.flashcard)
                }

                return flashcardViewHolder
            }
        }
    }

    override fun getItemId(position: Int): Long = getItem(position).flashcard.flashcardId

    fun getPosition(key: Long) = currentList.indexOfFirst { it.flashcard.flashcardId == key }

    fun selectAll(){
        for(item in currentList){
            selectionTracker?.select(item.flashcard.flashcardId)
        }
    }
}

class FlashcardsListDiffCallback : DiffUtil.ItemCallback<FlashcardAdapterItem>(){
    override fun areItemsTheSame(oldItem: FlashcardAdapterItem, newItem: FlashcardAdapterItem): Boolean {
        return oldItem.flashcard.flashcardId == newItem.flashcard.flashcardId
    }

    override fun areContentsTheSame(oldItem: FlashcardAdapterItem, newItem: FlashcardAdapterItem): Boolean {
        return oldItem.flashcard.question == newItem.flashcard.question &&  oldItem.flashcard.repetitionEnabled == newItem.flashcard.repetitionEnabled
    }
}