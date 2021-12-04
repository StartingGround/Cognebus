package com.startingground.cognebus.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val SORTING_ID = "sorting_id"
const val SORTING = "sorting"
const val SORTING_TYPE = "sorting_type"

@Entity(tableName = SORTING)
data class Sorting (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = SORTING_ID)
    val sortingId: Long,

    @ColumnInfo(name = SORTING_TYPE)
    var sortingType: String
)