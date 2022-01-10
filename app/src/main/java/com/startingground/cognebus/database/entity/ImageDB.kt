package com.startingground.cognebus.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "image", foreignKeys = [
    ForeignKey(
        entity = FlashcardDB::class,
        parentColumns = [FLASHCARD_ID],
        childColumns = [FLASHCARD_ID],
        onDelete = ForeignKey.SET_NULL,
        onUpdate = ForeignKey.SET_NULL
    )
])

data class ImageDB(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "image_id")
    val imageId: Long,

    @ColumnInfo(name = FLASHCARD_ID, defaultValue = "NULL")
    var flashcardId: Long?,

    @ColumnInfo(name = "file_extension")
    var fileExtension: String
)