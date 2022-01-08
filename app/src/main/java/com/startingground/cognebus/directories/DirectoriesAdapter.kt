package com.startingground.cognebus.directories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.startingground.cognebus.R
import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.Folder

class DirectoriesAdapter(
    private val directoriesFragment: DirectoriesFragment
) : ListAdapter<DirectoryItem, RecyclerView.ViewHolder>(DirectoriesDiffCallback()){

    var selectionTracker: SelectionTracker<String>? = null

    private val folderButtonContentDescriptionTemplate: String = directoriesFragment.getString(R.string.directories_folder_button_content_description_template)
    private val fileButtonContentDescriptionTemplate: String = directoriesFragment.getString(R.string.directories_file_button_content_description_template)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            DirectoriesFragment.TYPE_FOLDER -> FolderViewHolder.create(parent, directoriesFragment)
            DirectoriesFragment.TYPE_FILE -> FileViewHolder.create(parent, directoriesFragment)
            else -> throw Exception("Creating this view tye not supported")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            DirectoriesFragment.TYPE_FOLDER -> (holder as FolderViewHolder).bind(getItem(position), folderButtonContentDescriptionTemplate)
            DirectoriesFragment.TYPE_FILE -> (holder as FileViewHolder).bind(getItem(position), fileButtonContentDescriptionTemplate)
        }

        selectionTracker?.let{
            val selected = it.isSelected(getItem(position).itemId)
            (holder as ItemViewHolder).selected(selected)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position).content){
            is Folder -> DirectoriesFragment.TYPE_FOLDER
            is FileDB -> DirectoriesFragment.TYPE_FILE
            else -> throw Exception("Getting this item view type not supported")
        }
    }

    fun getItemPublic(position: Int): DirectoryItem = getItem(position)

    fun getPosition(key: String) = currentList.indexOfFirst { it.itemId == key }

    interface ItemViewHolder{
        fun getItemDetails() : ItemDetailsLookup.ItemDetails<String>
        fun selected(selected: Boolean)

        fun fileAndFolderSelectionHandler(button: MaterialButton, selected: Boolean) {
            button.isActivated = selected
            if (selected) {
                button.setIconResource(R.drawable.ic_check_circle_24)
                button.iconGravity = MaterialButton.ICON_GRAVITY_END
            } else {
                button.icon = null
            }
        }
    }

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemViewHolder{
        val folderButton: MaterialButton = itemView.findViewById(R.id.folder_button)
        var folderId: Long? = null
        var folderName: String? = null

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
            object : ItemDetailsLookup.ItemDetails<String>(){
                override fun getPosition(): Int = adapterPosition

                override fun getSelectionKey(): String = "fol" + folderId.toString()
            }

        fun bind(directoryItem: DirectoryItem, contentDescriptionTemplate: String){
            val folder = directoryItem.content as Folder
            folderButton.text = folder.name
            folderButton.contentDescription = contentDescriptionTemplate.format(folder.name)
            folderId = folder.folderId
            folderName = folder.name
        }

        override fun selected(selected: Boolean) {
            fileAndFolderSelectionHandler(folderButton, selected)
        }

        companion object{
            fun create(parent: ViewGroup, directoriesFragment: DirectoriesFragment): FolderViewHolder{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.folder_item, parent, false)
                val folderViewHolder = FolderViewHolder(view)
                folderViewHolder.folderButton.setOnClickListener {
                    directoriesFragment.onFolderButton(folderViewHolder.folderId, folderViewHolder.folderName)
                }
                return folderViewHolder
            }
        }
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ItemViewHolder{
        val fileButton: MaterialButton = itemView.findViewById(R.id.file_button)
        var fileId: Long? = null
        var fileName: String? = null

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> =
            object : ItemDetailsLookup.ItemDetails<String>(){
                override fun getPosition(): Int = adapterPosition

                override fun getSelectionKey(): String = "fil" + fileId.toString()
            }

        fun bind(directoryItem: DirectoryItem, contentDescriptionTemplate: String){
            val file = directoryItem.content as FileDB
            fileButton.text = file.name
            fileButton.contentDescription = contentDescriptionTemplate.format(file.name)
            fileId = file.fileId
            fileName = file.name
        }

        override fun selected(selected: Boolean) {
            fileAndFolderSelectionHandler(fileButton, selected)
        }

        companion object{
            fun create(parent: ViewGroup, directoriesFragment: DirectoriesFragment): FileViewHolder{
                val view = LayoutInflater.from(parent.context).inflate(R.layout.file_item, parent, false)
                val fileViewHolder = FileViewHolder(view)
                fileViewHolder.fileButton.setOnClickListener {
                    directoriesFragment.onFileButton(fileViewHolder.fileId, fileViewHolder.fileName)
                }
                return fileViewHolder
            }
        }
    }

    fun selectAll(){
        for(item in currentList){
            selectionTracker?.select(item.itemId)
        }
    }
}

class DirectoriesDiffCallback : DiffUtil.ItemCallback<DirectoryItem>(){
    override fun areItemsTheSame(oldItem: DirectoryItem, newItem: DirectoryItem): Boolean {
        return oldItem.itemId == newItem.itemId
    }

    override fun areContentsTheSame(oldItem: DirectoryItem, newItem: DirectoryItem): Boolean {
        return oldItem.isContentSame(newItem)
    }
}