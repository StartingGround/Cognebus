package com.startingground.cognebus.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.startingground.cognebus.database.entity.Folder

@Dao
interface FolderDatabaseDao {
    @Insert
    suspend fun insert(folder: Folder)

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)

    @Delete
    suspend fun deleteList(folders: List<Folder>)

    @Query("SELECT * FROM folder WHERE parent_folder_id = :folderId OR (parent_folder_id IS NULL AND :folderId IS NULL) ORDER BY name ASC")
    fun getFoldersByParentFolderId(folderId: Long?): LiveData<List<Folder>>

    @Query("SELECT * FROM folder WHERE (parent_folder_id = :folderId OR (parent_folder_id IS NULL AND :folderId IS NULL)) AND name = :name LIMIT 1")
    suspend fun getFolderByParentFolderIdAndName(folderId: Long?, name: String): Folder?
}