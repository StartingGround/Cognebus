package com.startingground.cognebus.directories

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
import com.startingground.cognebus.databinding.FragmentDirectoriesBinding

class DirectoriesFragment : Fragment() {

    companion object{
        const val TYPE_FOLDER = 1
        const val TYPE_FILE = 2
        const val TYPE_CREATE = 3
    }

    private lateinit var binding: FragmentDirectoriesBinding
    private lateinit var adapter: DirectoriesAdapter
    private lateinit var directoriesViewModel: DirectoriesViewModel
    private lateinit var dataViewModel: DataViewModel

    private var selectionTracker: SelectionTracker<String>? = null

    private var title: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_directories,
            container,
            false
        )

        val application = requireNotNull(this.activity).application
        val database = CognebusDatabase.getInstance(application)
        var folderId: Long? = null

        arguments?.let{
            folderId = it.getLong("folderId")
            if(it.getBoolean("folderIdIsNull")) {
                folderId = null
            }
            val defaultTitle = getString(R.string.directories_title_files)
            title = it.getString("title", defaultTitle)
        }

        val dataViewModelFactory = DataViewModelFactory(application)
        dataViewModel = ViewModelProvider(this.requireActivity(), dataViewModelFactory)
            .get(DataViewModel::class.java)

        val directoriesViewModelFactory = DirectoriesViewModelFactory(database, folderId, dataViewModel)
        directoriesViewModel = ViewModelProvider(this, directoriesViewModelFactory)
            .get(DirectoriesViewModel::class.java)

        adapter = DirectoriesAdapter(this)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.directoriesRecyclerView.adapter = adapter
        binding.directoriesRecyclerView.setHasFixedSize(true)

        binding.topAppBar.title = title

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        directoriesViewModel.folders.observe(viewLifecycleOwner) {
            refreshRecyclerViewItems()
        }

        directoriesViewModel.files.observe(viewLifecycleOwner){
            refreshRecyclerViewItems()
        }

        selectionTracker = directoriesViewModel.getSelectionTracker(binding.directoriesRecyclerView, adapter)

        adapter.selectionTracker = selectionTracker
        selectionTracker?.addObserver(selectionTrackerObserver)

        //If fragment is destroyed while selection is ongoing and recreated, we call this to show contextual top app bar again
        selectionTrackerObserver.onSelectionChanged()

    }


    private val selectionTrackerObserver = object : SelectionTracker.SelectionObserver<String>(){
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


    //Used to ensure simultaneous display of files and folders when opening fragment
    private var neitherOfListsIsLoaded = true

    private fun refreshRecyclerViewItems(){
        if(neitherOfListsIsLoaded){
            neitherOfListsIsLoaded = false
            return
        }

        val allItems: MutableList<DirectoryItem> = mutableListOf()

        directoriesViewModel.folders.value?.let {
            allItems += it.map { folder ->
                FolderItem(folder)
            }
        }
        directoriesViewModel.files.value?.let {
            allItems += it.map { file ->
                FileItem(file)
            }
        }
        allItems += CreateItem()
        adapter.submitList(allItems)
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
            mode?.menuInflater?.inflate(R.menu.directories_contextual_action_bar, menu)
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


    fun onCreateButton(){
        if((selectionTracker?.selection?.size() ?: 0) > 0) return

        val action = DirectoriesFragmentDirections.actionDirectoriesFragmentToCreateFolderOrFileSelectionFragment(
            directoriesViewModel.folderId ?: 0L,
            directoriesViewModel.folderId == null
        )
        findNavController().navigate(action)
    }

    fun onFolderButton(selectedFolderId: Long?, folderName: String?){
        if((selectionTracker?.selection?.size() ?: 0) > 0) return

        val action = DirectoriesFragmentDirections.actionDirectoriesFragmentSelf(
            selectedFolderId ?: 0L,
            selectedFolderId == null,
            folderName ?: ""
        )
        findNavController().navigate(action)
    }

    fun onFileButton(fileId: Long?, fileName: String?){
        if((selectionTracker?.selection?.size() ?: 0) > 0) return

        val action = DirectoriesFragmentDirections.actionDirectoriesFragmentToNavFile(fileId ?: 0, fileName)
        findNavController().navigate(action)
    }

    fun onDeleteButton(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.directories_delete_selected_items_dialog))
            .setNegativeButton(getString(R.string.directories_delete_dialog_negative_button)){ _, _ ->
            }
            .setPositiveButton(getString(R.string.directories_delete_dialog_positive_button)){ _, _ ->
                directoriesViewModel.deleteSelectedItems(adapter.currentList)
            }
            .show()
    }
}