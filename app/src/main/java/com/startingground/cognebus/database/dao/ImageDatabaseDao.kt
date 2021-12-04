package com.startingground.cognebus.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.startingground.cognebus.database.entity.ImageDB

@Dao
interface ImageDatabaseDao {
    @Insert
    suspend fun insert(image: ImageDB) : Long

    @Update
    suspend fun update(image: ImageDB)

    @Update
    suspend fun updateList(images: List<ImageDB>)

    @Delete
    suspend fun deleteList(images: List<ImageDB>)

    @Query("SELECT * FROM image WHERE flashcard_id IS NULL")
    suspend fun getUnusedImages(): List<ImageDB>

    @Query("SELECT image_id FROM image WHERE flashcard_id = :flashcardId")
    fun getImageIdsByFlashcardId(flashcardId: Long): LiveData<List<Long>>
}