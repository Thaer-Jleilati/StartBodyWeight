package com.epsilon.startbodyweight.selectorActivity

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.view.View
import android.widget.CompoundButton
import android.widget.Spinner
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.Exercise
import com.epsilon.startbodyweight.data.ExerciseEntity
import kotlin.math.max
import kotlin.math.min


class SelectorViewModel : ViewModel() {
    var mExerciseList = ArrayList<MutableLiveData<ExerciseEntity>>()

    fun populateExerciseListFromDB(myDBExercises: List<ExerciseEntity>, completeExerciseList: Map<Int, Exercise>) {
        // Load our progression list for each exercise from the JSON data and add it to our exercise data
        myDBExercises.forEach {
            var exerciseEntryInList = completeExerciseList[it.exerciseNum]
            it.allProgressions = exerciseEntryInList?.progs ?: ArrayList()

            val exerciseToAdd = MutableLiveData<ExerciseEntity>()
            exerciseToAdd.value = it
            mExerciseList.add(exerciseToAdd)
        }

    }

    fun populateExerciseListFromJson(completeExerciseList: Map<Int, Exercise>) {
        ExerData.convertToExerciseEntityList(completeExerciseList).forEach {
            val exerciseToAdd = MutableLiveData<ExerciseEntity>()
            exerciseToAdd.value = it
            mExerciseList.add(exerciseToAdd)
        }
    }

    private fun updateRepsInExercise(exercise: ExerciseEntity, reps1: Int, reps2: Int, reps3: Int) {
        exercise.set1Reps = reps1
        exercise.set2Reps = reps2
        exercise.set3Reps = reps3
    }

    fun incrementSet(exerciseLiveData: MutableLiveData<ExerciseEntity>, smallIncrement: Boolean = true) {
        exerciseLiveData.value?.let { exercise ->
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

            // Update our livedata
            exerciseLiveData.value = exercise
        }
    }

    fun decrementSet(exerciseLiveData: MutableLiveData<ExerciseEntity>, smallDecrement: Boolean = true) {
        exerciseLiveData.value?.let { exercise ->
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

            // Update our livedata
            exerciseLiveData.value = exercise
        }
    }

    fun onItemSelectedSpinner(parent: View, position: Int, exercise: ExerciseEntity) {
        exercise.progressionNumber = position
        exercise.progressionName = (parent as Spinner).selectedItem.toString()
    }

    fun onCheckedChanged(c: CompoundButton, isChecked: Boolean, exerciseLiveData: MutableLiveData<ExerciseEntity>) {
        exerciseLiveData.value?.let { exercise ->
            exercise.isActive = isChecked
            exerciseLiveData.value = exercise
        }
    }
}