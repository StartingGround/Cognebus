package com.startingground.cognebus.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.startingground.cognebus.database.entity.Sorting

@Dao
interface SortingDatabaseDao {
    @Query("SELECT * FROM sorting ORDER BY sorting_id ASC")
    fun getAll(): LiveData<List<Sorting>>
}