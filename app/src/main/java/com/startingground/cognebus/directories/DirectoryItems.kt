package com.startingground.cognebus.directories

import com.startingground.cognebus.database.entity.FileDB
import com.startingground.cognebus.database.entity.Folder

interface DirectoryItem{
    val itemId: String
    val content: Any

    fun isContentSame(otherItem: DirectoryItem): Boolean
}

class FolderItem(folder: Folder) : DirectoryItem{
    override val itemId: String = "fol" + folder.folderId.toString()
    override val content: Any = folder

    override fun isContentSame(otherItem: DirectoryItem): Boolean {
        if(otherItem.content is Folder){
            return (content as Folder).name == (otherItem.content as Folder).name
        }
        return false
    }
}

class FileItem(file: FileDB) : DirectoryItem{
    override val itemId: String = "fil" + file.fileId.toString()
    override val content: Any = file

    override fun isContentSame(otherItem: DirectoryItem): Boolean {
        if(otherItem.content is FileDB){
            return (content as FileDB).name == (otherItem.content as FileDB).name
        }
        return false
    }
}

class CreateItem : DirectoryItem{

    companion object{
        const val CREATE = "create"
    }

    override val itemId: String = CREATE
    override val content: Any = CREATE

    override fun isContentSame(otherItem: DirectoryItem): Boolean {
        return otherItem.content == CREATE
    }
}