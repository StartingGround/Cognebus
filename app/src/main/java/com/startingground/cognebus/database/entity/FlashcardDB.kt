package com.startingground.cognebus.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.startingground.cognebus.utilities.TimeCognebusUtils

const val FLASHCARD_ID = "flashcard_id"

@Entity(tableName = "flashcard", foreignKeys = [
    ForeignKey(
        entity = FileDB::class,
        parentColumns = [FILE_ID],
        childColumns = [FILE_ID],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )
])

data class FlashcardDB(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = FLASHCARD_ID)
    val flashcardId: Long,

    @ColumnInfo(name = "question")
    var question: String,

    @ColumnInfo(name = "answer")
    var answer: String,

    @ColumnInfo(name = FILE_ID)
    var fileId: Long,

    @ColumnInfo(name = "repetition_enabled")
    var repetitionEnabled: Boolean = true,

    @ColumnInfo(name = "repetition_date")
    var repetitionDate: Long = TimeCognebusUtils.getBeginningOfCurrentDay().timeInMillis,

    @ColumnInfo(name = "last_increase")
    var lastIncrease: Int = 0,

    //Used when not in remembering mode, so user can go again trough incorrectly answered questions
    @ColumnInfo(name = "answered_in_practice")
    var answeredInPractice: Boolean = false
)