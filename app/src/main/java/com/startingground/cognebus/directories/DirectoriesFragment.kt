package com.startingground.cognebus.directories

import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.startingground.cognebus.*
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.Folder
import com.startingground.cognebus.databinding.FragmentDirectoriesBinding
import com.startingground.cognebus.sharedviewmodels.ClipboardViewModel
import com.startingground.cognebus.sharedviewmodels.ClipboardViewModelFactory
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.sharedviewmodels.DataViewModelFactory

class DirectoriesFragment : Fragment() {

    companion object{
        const val TYPE_FOLDER = 1
        const val TYPE_FILE = 2
    }

    private lateinit var binding: FragmentDirectoriesBinding
    private lateinit var adapter: DirectoriesAdapter
    private lateinit var directoriesViewModel: DirectoriesViewModel
    private lateinit var dataViewModel: DataViewModel
    private lateinit var sharedClipboardViewModel: ClipboardViewModel

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

        val sharedClipboardViewModelFactory = ClipboardViewModelFactory(database, dataViewModel)
        sharedClipboardViewModel = ViewModelProvider(requireActivity(), sharedClipboardViewModelFactory).get(ClipboardViewModel::class.java)

        val folderContentDescription: String = getString(R.string.directories_folder_button_content_description_template)
        val fileContentDescription: String = getString(R.string.directories_file_button_content_description_template)
        adapter = DirectoriesAdapter(folderContentDescription, fileContentDescription)

        requireActivity().onBackPressedDispatcher.addCallback(this){
            if(sharedClipboardViewModel.pasteInProgress.value == true) return@addCallback
            isEnabled = false
            activity?.onBackPressed()
        }

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.clipboardViewModel = sharedClipboardViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.directoriesRecyclerView.adapter = adapter
        binding.directoriesRecyclerView.setHasFixedSize(true)

        binding.topAppBar.title = title

        binding.topAppBar.setNavigationOnClickListener {
            if(sharedClipboardViewModel.pasteInProgress.value == true) return@setNavigationOnClickListener
            findNavController().popBackStack()
        }

        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            if(sharedClipboardViewModel.pasteInProgress.value == true) return@setOnMenuItemClickListener true
            when(menuItem.itemId){
                R.id.paste -> {
                    onPasteButton()
                    true
                }
                else -> false
            }
        }

        sharedClipboardViewModel.thereIsContentReadyToBePastedInDirectories.observe(viewLifecycleOwner){
            binding.topAppBar.menu.findItem(R.id.paste).isEnabled = it
        }

        sharedClipboardViewModel.errorCopyingToSourceFolder.observe(viewLifecycleOwner){
            if(!it) return@observe

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.directories_error_copying_to_source_folder_message)
                .setPositiveButton(R.string.directories_error_copying_to_source_folder_positive_button){ _, _ ->}
                .show()

            sharedClipboardViewModel.removeErrorCopyingToSourceFolder()
        }

        sharedClipboardViewModel.foldersWillBeMergedAndFilesReplacedWarning.observe(viewLifecycleOwner){
            val (foldersWillBeMerged, filesWillBeReplaced) = it
            if(!foldersWillBeMerged && !filesWillBeReplaced) return@observe

            sharedClipboardViewModel.removeFoldersWillBeMergedAndFilesReplacedWarning()

            val title = when{
                foldersWillBeMerged && !filesWillBeReplaced -> R.string.directories_folder_merge_warning
                !foldersWillBeMerged && filesWillBeReplaced -> R.string.directories_file_replacement_warning
                else -> R.string.directories_folder_merge_and_file_replacement_warning
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setPositiveButton(R.string.directories_merge_and_replacement_warning_positive_button){ _, _ ->
                    onPasteButton(true)
                }
                .setNegativeButton(R.string.directories_merge_and_replacement_warning_negative_button){ _, _ ->}
                .show()
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

        binding.createButton.setOnClickListener { onCreateButton() }

        adapter.folderButtonClicked.observe(viewLifecycleOwner){ data ->
            data?.let {
                val folderId = it.first
                val folderName = it.second
                onFolderButton(folderId, folderName)
            }
        }

        adapter.fileButtonClicked.observe(viewLifecycleOwner){ data ->
            data?.let {
                val fileId = it.first
                val fileName = it.second
                onFileButton(fileId, fileName)
            }
        }

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
        adapter.submitList(allItems)
    }


    private var actionMode: ActionMode? = null

    private fun showContextualBar(numberOfSelectedItems: Int){
        if(actionMode == null) {
            actionMode = (activity as AppCompatActivity).startSupportActionMode(contextualBarCallback)
        }
        actionMode?.title = getString(R.string.selected_counter, numberOfSelectedItems)

        actionMode?.menu?.findItem(R.id.rename)?.isEnabled = numberOfSelectedItems == 1
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
            if(sharedClipboardViewModel.pasteInProgress.value == true) return true

            return when(item?.itemId){
                R.id.delete -> {
                    onDeleteButton()
                    true
                }
                R.id.select_all -> {
                    adapter.selectAll()
                    true
                }
                R.id.cut -> {
                    onCopyButton(adapter.currentList, cutEnabled = true)
                    selectionTracker?.clearSelection()
                    true
                }
                R.id.copy -> {
                    onCopyButton(adapter.currentList)
                    selectionTracker?.clearSelection()
                    true
                }
                R.id.rename -> {
                    onRenameButton()
                    selectionTracker?.clearSelection()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            selectionTracker?.clearSelection()
        }
    }


    private fun onCreateButton(){
        if(sharedClipboardViewModel.pasteInProgress.value == true) return

        if((selectionTracker?.selection?.size() ?: 0) > 0) return

        val action = DirectoriesFragmentDirections.actionDirectoriesFragmentToCreateFolderOrFileSelectionFragment(
            directoriesViewModel.folderId ?: 0L,
            directoriesViewModel.folderId == null
        )
        findNavController().navigate(action)
    }

    private fun onFolderButton(selectedFolderId: Long, folderName: String){
        if(sharedClipboardViewModel.pasteInProgress.value == true) return

        adapter.clearButtonTriggers()

        if((selectionTracker?.selection?.size() ?: 0) > 0) return

        val action = DirectoriesFragmentDirections.actionDirectoriesFragmentSelf(
            selectedFolderId,
            false,
            folderName
        )
        findNavController().navigate(action)
    }

    private fun onFileButton(fileId: Long, fileName: String){
        if(sharedClipboardViewModel.pasteInProgress.value == true) return

        adapter.clearButtonTriggers()

        if((selectionTracker?.selection?.size() ?: 0) > 0) return

        val action = DirectoriesFragmentDirections.actionDirectoriesFragmentToNavFile(fileId, fileName)
        findNavController().navigate(action)
    }

    private fun onDeleteButton(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.directories_delete_selected_items_dialog))
            .setNegativeButton(getString(R.string.directories_delete_dialog_negative_button)){ _, _ ->
            }
            .setPositiveButton(getString(R.string.directories_delete_dialog_positive_button)){ _, _ ->
                directoriesViewModel.deleteSelectedItems(adapter.currentList)
            }
            .show()
    }

    private fun onCopyButton(availableItems: List<DirectoryItem>, cutEnabled: Boolean = false){
        val selectedFileItems: List<DirectoryItem> = availableItems.filter { it.content is FileDB && (selectionTracker?.isSelected(it.itemId) ?: false) }
        val selectedFiles: List<FileDB> = selectedFileItems.map { it.content as FileDB }

        val selectedFolderItems: List<DirectoryItem> = availableItems.filter { it.content is Folder && (selectionTracker?.isSelected(it.itemId) ?: false)}
        val selectedFolders: List<Folder> = selectedFolderItems.map { it.content as Folder }

        sharedClipboardViewModel.copySelectedFilesAndFolders(selectedFiles, selectedFolders, cutEnabled = cutEnabled)
    }

    private fun onPasteButton(folderMergeAndFileReplacementApproved: Boolean = false){
        sharedClipboardViewModel.pasteSelectedToFolder(directoriesViewModel.folderId, folderMergeAndFileReplacementApproved)
    }


    private fun onRenameButton(){
        val numberOfSelectedItems: Int = selectionTracker?.selection?.size() ?: 0
        if(numberOfSelectedItems != 1) return

        val selectedDirectoryItem = adapter.currentList.find { selectionTracker?.isSelected(it.itemId) ?: false } ?: return

        if(selectedDirectoryItem.content is Folder) {
            val action = DirectoriesFragmentDirections.actionDirectoriesFragmentToCreateOrRenameFolderOrFileFragment(
                directoriesViewModel.folderId ?: 0L,
                directoriesViewModel.folderId == null,
                TYPE_FOLDER,
                (selectedDirectoryItem.content as Folder).folderId
            )

            findNavController().navigate(action)
        }

        if(selectedDirectoryItem.content is FileDB){
            val action = DirectoriesFragmentDirections.actionDirectoriesFragmentToCreateOrRenameFolderOrFileFragment(
                directoriesViewModel.folderId ?: 0L,
                directoriesViewModel.folderId == null,
                TYPE_FILE,
                (selectedDirectoryItem.content as FileDB).fileId
            )

            findNavController().navigate(action)
        }

    }

}