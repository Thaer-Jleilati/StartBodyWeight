package com.epsilon.startbodyweight.WorkoutActivity

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.Exercise
import com.epsilon.startbodyweight.data.ExerciseEntity

class WorkoutViewModel : ViewModel() {
    var mExerciseList = MutableLiveData <ArrayList<ExerciseEntity>>()
    var mIncludeDips: Boolean = false

    init {
        mExerciseList.value = ArrayList()
    }

    private fun initializeExerciseEntity(exercise: ExerciseEntity) {
        exercise.nextSet1Reps = exercise.set1Reps
        exercise.nextSet2Reps = exercise.set2Reps
        exercise.nextSet3Reps = exercise.set3Reps
        exercise.nextSetTime = exercise.setTime
        exercise.nextProgressionName = exercise.progressionName
        exercise.nextProgressionNumber = exercise.progressionNumber
        exercise.nextNumAttempts = exercise.numAttempts
        exercise.isModified = false
        exercise.exerMessage = ""
        exercise.isSet1Complete = false
        exercise.isSet2Complete = false
        exercise.isSet3Complete = false
        exercise.isSetTimeComplete = false
    }

    fun populateExerciseListFromDB(myDBExercises: List<ExerciseEntity>, completeExerciseList: Map<Int, Exercise>) {
        myDBExercises.forEach {
            initializeExerciseEntity(it)

            // Load our progression list for each exercise from the JSON data and add it to our exercise data
            var exerciseEntryInList = completeExerciseList[it.exerciseNum]
            it.allProgressions = if (exerciseEntryInList != null) exerciseEntryInList.progs else ArrayList()
        }
    }


    fun setNextProgression(exercise: ExerciseEntity) {
        // Move up in our progession
        if (exercise.progressionNumber + 1 < exercise.allProgressions.size) {
            exercise.nextProgressionNumber = exercise.progressionNumber + 1
            exercise.nextProgressionName = exercise.allProgressions[exercise.nextProgressionNumber]
        }
        //If we have maxed out our progressions, stay on the final progression
        else {
            exercise.nextProgressionNumber = exercise.progressionNumber
            exercise.nextProgressionName = exercise.progressionName
        }
    }

    fun passExercise(exercise: ExerciseEntity) {
        exercise.isModified = true

        // Since we passed, reset our number of attempts
        exercise.nextNumAttempts = 0

        val moveToNextExercise = ExerData.incrementExercise(exercise)
        if (moveToNextExercise) {
            exercise.exerMessage = "Congrats. Moving up!"
            setNextProgression(exercise)
            // TODO: Support alternate dips / pushups
        } else {
            exercise.exerMessage = if (exercise.isTimedExercise) {
                "Way to go. Next time you'll do ${exercise.nextSetTime} seconds"
            } else {
                "Way to go. Next time you'll do ${exercise.nextSet1Reps} x ${exercise.nextSet2Reps} x ${exercise.nextSet3Reps}"
            }
        }
    }

    fun failExercise(exercise: ExerciseEntity) {
        exercise.isModified = true
        exercise.nextNumAttempts = exercise.numAttempts + 1
        exercise.exerMessage = "Failure is essential. Try again next workout."

        if (exercise.nextNumAttempts >= 2) {
            ExerData.decrementExercise(exercise)

            exercise.nextNumAttempts = 0
            exercise.exerMessage = if (exercise.isTimedExercise) {
                "2nd attempt at exercise. Lowering difficulty to ${exercise.nextSetTime} seconds"
            } else {
                "2nd attempt at exercise. Lowering difficulty to ${exercise.nextSet1Reps} x ${exercise.nextSet2Reps} x ${exercise.nextSet3Reps}"
            }
            // TODO: New progression too hard? Do the previous one up until 12 reps
        }
    }

    fun completeSet(exercise: ExerciseEntity, waitTime: Int) {
        if (exercise.isTimedExercise) {
            completeSetTimed(exercise)
        } else {
            completeSetReps(exercise, waitTime)
        }
    }

    private fun completeSetTimed(exercise: ExerciseEntity) {
        when {
            !exercise.isSetTimeComplete -> {
                exercise.isSetTimeComplete = true
                passExercise(exercise)
            }
            exercise.isSetTimeComplete -> {
                exercise.isSetTimeComplete = false
                initializeExerciseEntity(exercise)
            }
        }
    }

    private fun completeSetReps(exercise: ExerciseEntity, waitTime: Int) {
        when {
            !exercise.isSet1Complete -> {
                exercise.isSet1Complete = true
                exercise.exerMessage = "Congratulations. Please rest for $waitTime seconds."
            }
            !exercise.isSet2Complete -> {
                exercise.isSet2Complete = true
                exercise.exerMessage = "Congratulations. Please rest for $waitTime seconds."

            }
            !exercise.isSet3Complete -> {
                exercise.isSet3Complete = true
                passExercise(exercise)
            }
            else -> {
                exercise.isSet1Complete = false
                exercise.isSet2Complete = false
                exercise.isSet3Complete = false

                initializeExerciseEntity(exercise)
            }
        }
    }

    fun completeWorkout() {
        // Update our exercise with our results
        mExerciseList.value?.forEachIndexed { _, it ->
            // Automatically pass all untouched exercises
            if (!it.isModified) {
                passExercise(it)
            }

            it.progressionName = it.nextProgressionName
            it.progressionNumber = it.nextProgressionNumber
            it.set1Reps = it.nextSet1Reps
            it.set2Reps = it.nextSet2Reps
            it.set3Reps = it.nextSet3Reps
            it.setTime = it.nextSetTime
            it.numAttempts = it.nextNumAttempts
        }
    }
}