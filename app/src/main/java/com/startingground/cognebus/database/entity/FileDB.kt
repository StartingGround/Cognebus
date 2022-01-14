package com.startingground.cognebus.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

const val FILE_ID = "file_id"

@Entity(tableName = "file", foreignKeys = [
    ForeignKey(
        entity = Folder::class,
        parentColumns = [FOLDER_ID],
        childColumns = [FOLDER_ID],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = Sorting::class,
        parentColumns = [SORTING_ID],
        childColumns = [SORTING_ID],
        onDelete = ForeignKey.NO_ACTION,
        onUpdate = ForeignKey.NO_ACTION
    )
])

data class FileDB(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = FILE_ID)
    val fileId: Long,

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = FOLDER_ID)
    var folderId: Long? = null,

    @ColumnInfo(name = SORTING_ID)
    var sortingId: Long = 1,

    @ColumnInfo(name = "enable_html")
    var enableHtml: Boolean = false,

    @ColumnInfo(name = "only_practice_enabled")
    var onlyPracticeEnabled: Boolean,

    @ColumnInfo(name = "repetition_enabled")
    var repetitionEnabled: Boolean,

    @ColumnInfo(name = "continue_repetition_after_default_period")
    var continueRepetitionAfterDefaultPeriod: Boolean,

    @ColumnInfo(name = "cycle_increment")
    var cycleIncrement: Int,

    @ColumnInfo(name = "max_days_per_cycle")
    var maxDaysPerCycle: Int
)