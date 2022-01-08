package com.startingground.cognebus.directories

import androidx.lifecycle.ViewModel
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.startingground.cognebus.sharedviewmodels.DataViewModel
import com.startingground.cognebus.database.CognebusDatabase
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.Folder

class DirectoriesViewModel(database: CognebusDatabase, val folderId: Long?, private val dataViewModel: DataViewModel) : ViewModel() {

    val folders = database.folderDatabaseDao.getLiveDataFoldersByParentFolderId(folderId)
    val files = database.fileDatabaseDao.getLiveDataFilesByFolderId(folderId)


    private var selectionTracker: SelectionTracker<String>? = null

    fun getSelectionTracker(recyclerView: RecyclerView, adapter: DirectoriesAdapter): SelectionTracker<String>? {

        val selectedItems = selectionTracker?.selection

        selectionTracker = SelectionTracker.Builder(
            "directoriesSelectionTracker",
            recyclerView,
            DirectoriesItemKeyProvider(adapter),
            DirectoriesItemDetailsLookup(recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            directoriesSelectionPredicate
        ).build()

        selectedItems?.forEach { selectionTracker?.select(it) }

        return selectionTracker
    }


    private val directoriesSelectionPredicate = object : SelectionTracker.SelectionPredicate<String>(){
        override fun canSetStateForKey(key: String, nextState: Boolean): Boolean {
            return key != DirectoriesItemDetailsLookup.EMPTY_DIRECTORIES_ITEM.NONE
        }

        override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean {
            return position != Integer.MAX_VALUE
        }

        override fun canSelectMultiple(): Boolean = true
    }


    fun deleteSelectedItems(items: List<DirectoryItem>){
        val selectedFiles: MutableList<FileDB> = mutableListOf()
        val selectedFolders: MutableList<Folder> = mutableListOf()

        for (item in items){
            if(selectionTracker?.isSelected(item.itemId) == true){
                when(item.content){
                    is FileDB -> selectedFiles.add(item.content as FileDB)
                    is Folder -> selectedFolders.add(item.content as Folder)
                }
            }
        }

        dataViewModel.deleteFileList(selectedFiles)
        dataViewModel.deleteFolderList(selectedFolders)
    }
}