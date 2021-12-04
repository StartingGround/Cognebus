package com.startingground.cognebus.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.startingground.cognebus.database.entity.FileDB

@Dao
interface FileDatabaseDao {
    @Insert
    suspend fun insert(file: FileDB)

    @Update
    suspend fun update(file: FileDB)

    @Delete
    suspend fun delete(file: FileDB)

    @Delete
    suspend fun deleteList(files: List<FileDB>)

    @Query("SELECT * FROM file WHERE folder_id = :folderId OR (folder_id IS NULL AND :folderId IS NULL)ORDER BY name ASC")
    fun getFilesByFolderId(folderId: Long?): LiveData<List<FileDB>>

    @Query("SELECT * FROM file WHERE file_id = :fileId")
    fun getFileByFileId(fileId: Long): LiveData<FileDB>?

    @Query("SELECT * FROM file WHERE (folder_id = :folderId OR (folder_id IS NULL AND :folderId IS NULL)) AND name = :name LIMIT 1")
    suspend fun getFileByFolderIdAndName(folderId: Long?, name: String): FileDB?

    @Query("SELECT * FROM file WHERE file_id IN (:fileIdList)")
    suspend fun getFilesInFileIdList(fileIdList: List<Long>): List<FileDB>
}