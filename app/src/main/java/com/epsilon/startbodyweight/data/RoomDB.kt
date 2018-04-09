package com.epsilon.startbodyweight.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.util.Log
import com.epsilon.startbodyweight.SelectorActivity

@Database(entities = [MyExercises::class], version = 5)
abstract class RoomDB : RoomDatabase() {
    val LTAG = SelectorActivity::class.qualifiedName

    abstract fun Dao(): Dao

    companion object {
        private var instance: RoomDB? = null

        fun get(context: Context?): RoomDB? {
            if (instance == null) {
                if (context == null){
                    Log.e(LTAG, "Received invalid context. Cannot build DB.")
                    return null
                }
                synchronized(RoomDB::class) {
                    Log.d(LTAG, "Building new Room DB instance....")
                    instance = Room.databaseBuilder(context, RoomDB::class.java, "sbw_database").
                            // Can implement if we update database
                            //addMigrations(MIGRATION_2_3).
                            fallbackToDestructiveMigration().
                            build()
                    Log.d(LTAG, "Built new Room DB instance.")
                }
            } else {
                Log.d(LTAG, "Returning Room DB instance")
            }

            return instance
        }

        fun destroyInstance() {
            instance = null
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // We cannot drop columns in sqlite... Just destroy and restart.
                database.execSQL("DELETE FROM MyExercises")
            }
        }
    }
}