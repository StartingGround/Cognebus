package com.startingground.cognebus.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.startingground.cognebus.database.entity.FlashcardDB

@Dao
interface FlashcardDatabaseDao {
    @Insert
    suspend fun insert(flashcard: FlashcardDB): Long

    @Update
    suspend fun update(flashcard: FlashcardDB)

    @Update
    suspend fun updateWithList(flashcards: List<FlashcardDB>)

    @Delete
    suspend fun delete(flashcard: FlashcardDB)

    @Delete
    suspend fun deleteList(flashcards: List<FlashcardDB>)

    @Query("SELECT * FROM flashcard WHERE flashcard_id = :flashcardId")
    suspend fun getFlashcardByFlashcardId(flashcardId: Long): FlashcardDB

    @Query("SELECT * FROM flashcard WHERE file_id = :fileId")
    suspend fun getFlashcardsByFileId(fileId: Long): List<FlashcardDB>

    @Query("SELECT * FROM flashcard WHERE file_id = :fileId")
    fun getLiveDataFlashcardsByFileId(fileId: Long): LiveData<List<FlashcardDB>>

    @Query( "SELECT * FROM flashcard WHERE repetition_date < :date AND repetition_enabled = 1 AND " +
            "(" +
                "(last_increase <= 30 AND file_id IN (SELECT file_id FROM file WHERE repetition_enabled = 1))" +
                " OR " +
                "(last_increase > 30 AND file_id IN (SELECT file_id FROM file WHERE repetition_enabled = 1 AND continue_repetition_after_default_period = 1))" +
            ")")
    fun getFlashcardsForRepetition(date: Long): LiveData<List<FlashcardDB>>
}