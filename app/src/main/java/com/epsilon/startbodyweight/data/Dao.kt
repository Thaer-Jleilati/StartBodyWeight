package com.epsilon.startbodyweight.data

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateExercise(exercise: MyExercises): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateAll(exercises: List<MyExercises>): List<Long>

    @Query("SELECT * from myExercises")
    fun getAllMyExercises(): List<MyExercises>

    @Query("DELETE FROM myExercises")
    fun nukeTable()

    @Query("SELECT count(numAttempts) FROM myExercises")
    fun numRows() : Int
}