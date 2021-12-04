package com.startingground.cognebus.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

const val FOLDER_ID = "folder_id"
const val PARENT_FOLDER_ID = "parent_folder_id"

@Entity(tableName = "folder", foreignKeys = [
    ForeignKey(
        entity = Folder::class,
        parentColumns = [FOLDER_ID],
        childColumns = [PARENT_FOLDER_ID],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )
])

data class Folder (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = FOLDER_ID)
    val folderId: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = PARENT_FOLDER_ID)
    var parentFolderId: Long? = null
)