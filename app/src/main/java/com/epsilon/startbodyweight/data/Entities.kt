package com.epsilon.startbodyweight.data


import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

@Entity
data class MyExercises (
        @PrimaryKey var exerciseName: String = "",
        var exerciseNum: Int = 0,
        var progressionName: String = "",
        var progressionNumber: Int = 0,
        var set1Reps: Int = 0,
        var set2Reps: Int = 0,
        var set3Reps: Int = 0,
        var setTime: Int = 0,
        var isTimedExercise: Boolean = false,
        var numAttempts: Int = 0,

        // Do not store the following items in our DB
        //TODO remove next set reps?
        @Ignore var nextSet1Reps: Int = 0,
        @Ignore var nextSet2Reps: Int = 0,
        @Ignore var nextSet3Reps: Int = 0,
        @Ignore var nextSetTime: Int = 0,
        @Ignore var allProgressions: ArrayList<String>? = ArrayList()
)
