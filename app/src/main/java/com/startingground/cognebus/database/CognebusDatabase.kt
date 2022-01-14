package com.startingground.cognebus.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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

    companion object{
        @Volatile
        private var INSTANCE: CognebusDatabase? = null
        fun getInstance(context: Context): CognebusDatabase{
            synchronized(this){
                var instance = INSTANCE

                if(instance == null){

                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        CognebusDatabase::class.java,
                        "cognebus_database"
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .addCallback(callback).build()

                    INSTANCE = instance

                }
                return instance
            }
        }

        private val callback = object : RoomDatabase.Callback(){
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                val allSortingTypes: List<String> = listOf(
                    "In Order",
                    "Reverse Order",
                    "Shuffled"
                )

                var queryValues = ""
                repeat(allSortingTypes.size){
                    queryValues += " (\"${allSortingTypes[it]}\")"
                    if(it != (allSortingTypes.size - 1)){
                        queryValues += ","
                    }
                }

                val queryString = "INSERT INTO $SORTING ($SORTING_TYPE) VALUES $queryValues"
                db.execSQL(queryString)
            }
        }
    }
}

private val MIGRATION_1_2 = object : Migration(1,2){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE image ADD COLUMN file_extension TEXT NOT NULL DEFAULT 'jpg'")
    }
}

private val MIGRATION_2_3 = object : Migration(2,3){
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE file ADD COLUMN only_practice_enabled INTEGER NOT NULL DEFAULT 0")
    }
}