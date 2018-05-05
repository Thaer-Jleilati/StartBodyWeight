package com.epsilon.startbodyweight.data

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateAll(exercises: List<ExerciseEntity>): List<Long>

    @Query("SELECT * from ExerciseEntity ORDER BY exerciseNum")
    fun getAllMyExercises(): List<ExerciseEntity>

    @Query("SELECT * from ExerciseEntity WHERE exerciseName != 'Dips' ORDER BY exerciseNum")
    fun getAllMyExercisesExceptDips(): List<ExerciseEntity>

    @Query("DELETE FROM ExerciseEntity")
    fun nukeTable()

    @Query("SELECT count(numAttempts) FROM ExerciseEntity")
    fun numRows() : Int
}