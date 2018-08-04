package com.epsilon.startbodyweight.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.Exercise
import com.epsilon.startbodyweight.data.ExerciseEntity
import kotlin.math.max
import kotlin.math.min

class SelectorViewModel : ViewModel() {
    var mExerciseList = MutableLiveData<ArrayList<ExerciseEntity>>()

    init {
        mExerciseList.value = ArrayList()
    }

    fun populateExerciseListFromDB(myDBExercises: List<ExerciseEntity>, completeExerciseList: Map<Int, Exercise>) {
        // Load our progression list for each exercise from the JSON data and add it to our exercise data
        myDBExercises.forEach {
            var exerciseEntryInList = completeExerciseList[it.exerciseNum]
            it.allProgressions = if (exerciseEntryInList != null) exerciseEntryInList.progs else ArrayList()
        }
        mExerciseList.value?.addAll(myDBExercises)
    }

    fun populateExerciseListFromJson(completeExerciseList: Map<Int, Exercise>) {
        mExerciseList.value?.addAll(ExerData.convertToExerciseEntityList(completeExerciseList))
    }

    private fun updateRepsInExercise(exercise: ExerciseEntity, reps1: Int, reps2: Int, reps3: Int) {
        exercise.set1Reps = reps1
        exercise.set2Reps = reps2
        exercise.set3Reps = reps3
    }

    fun incrementSet(exercise: ExerciseEntity, smallIncrement: Boolean) {
        if (exercise.isTimedExercise) {
            val timeIncrement = if (smallIncrement) 5 else 10
            exercise.setTime = min(exercise.setTime + timeIncrement, ExerData.MAX_EXERCISE_TIME)
        } else {
            var (numReps1, numReps2, numReps3) =
                    if (smallIncrement) {
                        ExerData.computeSmallExerciseIncrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    } else {
                        ExerData.computeBigExerciseIncrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    }
            if (ExerData.exceededMaxReps(numReps1, numReps2, numReps3)) {
                numReps1 = ExerData.MAX_EXERCISE_REPS
                numReps2 = ExerData.MAX_EXERCISE_REPS
                numReps3 = ExerData.MAX_EXERCISE_REPS
            }

            updateRepsInExercise(exercise, numReps1, numReps2, numReps3)
        }
    }

    fun decrementSet(exercise: ExerciseEntity, smallDecrement: Boolean) {
        if (exercise.isTimedExercise) {
            val timeDecrement = if (smallDecrement) 5 else 10
            exercise.setTime = max(exercise.setTime - timeDecrement, ExerData.MIN_EXERCISE_TIME)
        } else {
            var (numReps1, numReps2, numReps3) =
                    if (smallDecrement) {
                        ExerData.computeSmallExerciseDecrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    } else {
                        ExerData.computeBigExerciseDecrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    }
            if (ExerData.deceededMinReps(numReps1, numReps2, numReps3)) {
                numReps1 = ExerData.MIN_EXERCISE_REPS
                numReps2 = ExerData.MIN_EXERCISE_REPS
                numReps3 = ExerData.MIN_EXERCISE_REPS
            }

            updateRepsInExercise(exercise, numReps1, numReps2, numReps3)
        }
    }
}