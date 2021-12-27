package com.startingground.cognebus.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.startingground.cognebus.database.entity.Folder

@Dao
interface FolderDatabaseDao {
    @Insert
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)

    @Delete
    suspend fun deleteList(folders: List<Folder>)

    @Query("SELECT * FROM folder WHERE folder_id = :folderId OR (folder_id IS NULL AND :folderId IS NULL)")
    suspend fun getFolderByFolderId(folderId: Long?): Folder

    @Query("SELECT * FROM folder WHERE parent_folder_id = :folderId OR (parent_folder_id IS NULL AND :folderId IS NULL) ORDER BY name ASC")
    suspend fun getFoldersByParentFolderId(folderId: Long?): List<Folder>

    @Query("SELECT * FROM folder WHERE parent_folder_id = :folderId OR (parent_folder_id IS NULL AND :folderId IS NULL) ORDER BY name ASC")
    fun getLiveDataFoldersByParentFolderId(folderId: Long?): LiveData<List<Folder>>

    @Query("SELECT * FROM folder WHERE (parent_folder_id = :folderId OR (parent_folder_id IS NULL AND :folderId IS NULL)) AND name = :name LIMIT 1")
    suspend fun getFolderByParentFolderIdAndName(folderId: Long?, name: String): Folder?

    @Query("SELECT * FROM folder WHERE folder_id IN (:folderIdList)")
    fun getLiveDataFoldersInFolderIdList(folderIdList: List<Long>): LiveData<List<Folder>>
}