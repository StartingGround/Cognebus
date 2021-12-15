package com.startingground.cognebus.flashcardslist

import androidx.lifecycle.*
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.startingground.cognebus.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import kotlinx.coroutines.launch

class FlashcardsListViewModel(
    database: CognebusDatabase,
    fileId: Long,
    private val dataViewModel: DataViewModel,
    private val enableHtml: Boolean
    ): ViewModel() {

    private val flashcards = database.flashcardDatabaseDao.getFlashcardsByFileId(fileId)
    private val _flashcardsAdapter: MutableLiveData<List<FlashcardAdapterItem>> = MutableLiveData()
    val flashcardAdapter: LiveData<List<FlashcardAdapterItem>> get() = _flashcardsAdapter

    private val flashcardObserver = Observer<List<FlashcardDB>>{
        viewModelScope.launch {
             _flashcardsAdapter.value = it.map { flashcard ->
                val questionText = dataViewModel.prepareStringForPracticeCaller(flashcard.question, enableHtml)
                FlashcardAdapterItem(questionText, flashcard)
            }
        }
    }

    init {
        flashcards.observeForever(flashcardObserver)
    }

    override fun onCleared() {
        super.onCleared()
        flashcards.removeObserver(flashcardObserver)
    }


    private var selectionTracker: SelectionTracker<Long>? = null


    fun getSelectionTracker(recyclerView: RecyclerView, adapter: FlashcardsListAdapter): SelectionTracker<Long>? {

        val selectedItems = selectionTracker?.selection

        selectionTracker = SelectionTracker.Builder(
            "flashcardsListTracker",
            recyclerView,
            FlashcardsKeyProvider(adapter),
            FlashcardDetailsLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(
            flashcardSelectionPredicate
        ).build()

        selectedItems?.forEach { selectionTracker?.select(it) }

        return selectionTracker
    }


    private val flashcardSelectionPredicate = object : SelectionTracker.SelectionPredicate<Long>(){
        override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean {
            return key != 0L
        }

        override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean {
            return position != Integer.MAX_VALUE
        }

        override fun canSelectMultiple(): Boolean = true
    }


    fun deleteSelectedFlashcards(allFlashcards: List<FlashcardDB>){
        selectionTracker?.let { tracker ->
            val selectedFlashcards = allFlashcards.filter{
                tracker.isSelected(it.flashcardId)
            }

            //If we skip deselection app will crash when in list of N flashcards we delete N flashcards
            //This happens because selection tracker will be removing selected items gradually, N-1, then another change N-2, ...
            // But all flashcards are already deleted and because of that fragment is popped from backstack and it happens probably
            // while ui is deleted and flashcardsListViewModel still alive and tries to refresh contextual bars selected item counter,
            // which doesn't exist at that moment.
            for(flashcard in selectedFlashcards){
                tracker.deselect(flashcard.flashcardId)
            }

            dataViewModel.deleteFlashcardList(selectedFlashcards)
        }
    }
}