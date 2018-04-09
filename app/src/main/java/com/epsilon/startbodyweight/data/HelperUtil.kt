package com.epsilon.startbodyweight.data

import android.content.res.Resources

class HelperUtil {
    companion object {
        fun populateDbTestData(db: RoomDB?, resources: Resources) {
            val exers = JSONdata.getExerciseList(resources)
            exers?.
                    mapIndexed { i, it -> MyExercises( it.name, i, it.progs[0], 0, 5, 6, 2, 0, false, 0) }?.
                    forEach { db?.Dao()?.updateExercise(it) }
        }

        fun nukeDatabase(db: RoomDB?) {
            db?.Dao()?.nukeTable()
        }

        fun isDbInitialized(db: RoomDB?) : Boolean {
            return db?.Dao()?.numRows() ?: 0 > 0
        }
    }
}