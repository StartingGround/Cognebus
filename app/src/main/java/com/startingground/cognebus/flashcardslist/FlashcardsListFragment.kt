package com.startingground.cognebus.flashcardslist

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.startingground.cognebus.DataViewModel
import com.startingground.cognebus.DataViewModelFactory
import com.startingground.cognebus.R
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FlashcardDB
import com.startingground.cognebus.databinding.FragmentFlashcardsListBinding

class FlashcardsListFragment : Fragment() {

    private lateinit var adapter: FlashcardsListAdapter
    private lateinit var flashcardsListViewModel: FlashcardsListViewModel
    private lateinit var dataViewModel: DataViewModel

    private lateinit var binding: FragmentFlashcardsListBinding

    private var selectionTracker: SelectionTracker<Long>? = null
    private var fileId = 0L
    private var title = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var enableHtml = false

        arguments?.let{
            fileId = it.getLong("fileId")
            enableHtml = it.getBoolean("enableHtml")
            title = it.getString("title", "")
        }

        val application = requireNotNull(this.activity).application
        val database = CognebusDatabase.getInstance(application)

        val dataViewModelFactory = DataViewModelFactory(application)
        dataViewModel = ViewModelProvider(this.requireActivity(), dataViewModelFactory).get(DataViewModel::class.java)

        val flashcardsListViewModelFactory = FlashcardsListVieModelFactory(database, fileId, dataViewModel, enableHtml)
        flashcardsListViewModel = ViewModelProvider(this, flashcardsListViewModelFactory).get(FlashcardsListViewModel::class.java)

        adapter = FlashcardsListAdapter(this)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_flashcards_list, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.flashcardsListRecyclerView.adapter = adapter
        binding.flashcardsListRecyclerView.setHasFixedSize(true)
        binding.flashcardsListRecyclerView.setItemViewCacheSize(10)

        binding.topAppBar.title = title

        binding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        flashcardsListViewModel.flashcardAdapter.observe(viewLifecycleOwner){
            adapter.submitList(it)
            if(it.isEmpty()){
                findNavController().popBackStack()
            }
        }

        selectionTracker = flashcardsListViewModel.getSelectionTracker(binding.flashcardsListRecyclerView, adapter)

        adapter.selectionTracker = selectionTracker
        selectionTracker?.addObserver(selectionTrackerObserver)

        //If fragment is destroyed while selection is ongoing and recreated, we call this to show contextual top app bar again
        selectionTrackerObserver.onSelectionChanged()
    }


    private val selectionTrackerObserver = object : SelectionTracker.SelectionObserver<Long>(){
        override fun onSelectionChanged() {
            super.onSelectionChanged()
            val numberOfSelectedItems = selectionTracker?.selection?.size() ?: 0
            if(numberOfSelectedItems > 0){
                showContextualBar(numberOfSelectedItems)
            } else{
                hideContextualBar()
            }
        }
    }


    private var actionMode: ActionMode? = null

    private fun showContextualBar(numberOfSelectedItems: Int){
        if(actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(contextualBarCallback)
        }
        actionMode?.title = getString(R.string.selected_counter, numberOfSelectedItems)
    }


    private fun hideContextualBar(){
        actionMode?.finish()
        actionMode = null
    }


    private val contextualBarCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.flashcards_list_contextual_action_bar, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when(item?.itemId){
                R.id.delete -> {
                    onDeleteButton()
                    true
                }
                R.id.select_all ->{
                    adapter.selectAll()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            selectionTracker?.clearSelection()
        }
    }


    fun onFlashcardClick(flashcard: FlashcardDB?){
        if((selectionTracker?.selection?.size() ?: 0) > 0) return

        val action = FlashcardsListFragmentDirections.actionFlashcardsListFragmentToFlashcardPagerFragment(fileId, flashcard?.flashcardId ?: 0L)
        findNavController().navigate(action)
    }


    fun onDeleteButton(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.flashcards_list_delete_selected_flashcards_dialog))
            .setNegativeButton(getString(R.string.flashcards_list_delete_dialog_negative_button)){ _, _ ->
            }
            .setPositiveButton(getString(R.string.flashcards_list_delete_dialog_positive_button)){ _, _ ->
                val flashcards = adapter.currentList.map { it.flashcard }
                flashcardsListViewModel.deleteSelectedFlashcards(flashcards)
            }
            .show()
    }
}