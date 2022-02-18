package com.startingground.cognebus.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.startingground.cognebus.database.dao.*
import com.startingground.cognebus.database.entity.*

@Database(
    entities = [FileDB::class, Folder::class, ImageDB::class, FlashcardDB::class, Sorting::class],
    version = 3,
    exportSchema = false
)
abstract class CognebusDatabase : RoomDatabase() {

    abstract val fileDatabaseDao: FileDatabaseDao
    abstract val folderDatabaseDao: FolderDatabaseDao
    abstract val imageDatabaseDao: ImageDatabaseDao
    abstract val flashcardDatabaseDao: FlashcardDatabaseDao
    abstract val sortingDatabaseDao: SortingDatabaseDao
}

val MIGRATION_1_2 = object : Migration(1,2){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE image ADD COLUMN file_extension TEXT NOT NULL DEFAULT 'jpg'")
    }
}

val MIGRATION_2_3 = object : Migration(2,3){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE file ADD COLUMN only_practice_enabled INTEGER NOT NULL DEFAULT 0")
    }
}