package com.startingground.cognebus.database

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CognebusDatabaseModule {

    @Singleton
    @Provides
    fun provideCognebusDatabase(application: Application): CognebusDatabase{
        return Room.databaseBuilder(
            application.applicationContext,
            CognebusDatabase::class.java,
            "cognebus_database"
        ).createFromAsset("database/cognebus_database.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

}