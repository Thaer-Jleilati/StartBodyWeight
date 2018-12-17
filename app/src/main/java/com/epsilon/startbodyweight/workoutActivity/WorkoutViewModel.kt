package com.epsilon.startbodyweight.workoutActivity

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.Exercise
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.data.ExerciseSetState
import com.epsilon.startbodyweight.notif.SingleLiveEvent

class WorkoutViewModel : ViewModel() {
    var mExerciseList = ArrayList<MutableLiveData<ExerciseEntity>>()
    var mWaitTimeRegular: Int = 0
    var mWaitTimeFailed: Int = 0
    private val _setCompletionEvent = SingleLiveEvent<ExerciseEntity>()
    val setCompletionEvent: LiveData<ExerciseEntity>
        get() = _setCompletionEvent


    private fun initializeExerciseEntity(exercise: ExerciseEntity) {
        exercise.nextSet1Reps = exercise.set1Reps
        exercise.nextSet2Reps = exercise.set2Reps
        exercise.nextSet3Reps = exercise.set3Reps
        exercise.nextSetTime = exercise.setTime
        exercise.nextProgressionName = exercise.progressionName
        exercise.nextProgressionNumber = exercise.progressionNumber
        exercise.nextNumAttempts = exercise.numAttempts
        exercise.exerMessage = ""
        exercise.set1State = ExerciseSetState.NOT_STARTED
        exercise.set2State = ExerciseSetState.NOT_STARTED
        exercise.set3State = ExerciseSetState.NOT_STARTED
        exercise.setTimedState = ExerciseSetState.NOT_STARTED
        exercise.nextSetRestTime = 0
    }

    fun populateExerciseListFromDB(myDBExercises: List<ExerciseEntity>, completeExerciseList: Map<Int, Exercise>) {
        // Only keep active exercises
        myDBExercises.filter { it.isActive }.forEach {
            initializeExerciseEntity(it)

            // Load our progression list for each exercise from the JSON data and add it to our exercise data
            var exerciseEntryInList = completeExerciseList[it.exerciseNum]
            it.allProgressions = exerciseEntryInList?.progs ?: ArrayList()

            val exerciseToAdd = MutableLiveData<ExerciseEntity>()
            exerciseToAdd.value = it
            mExerciseList.add(exerciseToAdd)
        }
    }


    private fun setNextProgression(exercise: ExerciseEntity) {
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

    fun failExercise(exercise: ExerciseEntity) {
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


    private fun passExercise(exercise: ExerciseEntity) {
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

    fun failSet(exerciseLiveData: MutableLiveData<ExerciseEntity>) {
        exerciseLiveData.value?.let { exercise ->
            exercise.nextSetRestTime = mWaitTimeFailed
            when {
                exercise.set1State == ExerciseSetState.NOT_STARTED -> {
                    exercise.set1State = ExerciseSetState.FAILED
                    exercise.exerMessage = "Please rest for ${exercise.nextSetRestTime} seconds."
                }
                exercise.set2State == ExerciseSetState.NOT_STARTED -> {
                    exercise.set2State = ExerciseSetState.FAILED
                    exercise.exerMessage = "Please rest for ${exercise.nextSetRestTime} seconds."

                }
                exercise.set3State == ExerciseSetState.NOT_STARTED -> {
                    exercise.set3State = ExerciseSetState.FAILED
                    failExercise(exercise)
                }
                else -> {
                    initializeExerciseEntity(exercise)
                }
            }

            // Tell our activity our set was completed, this allows it to handle the notifications
            _setCompletionEvent.value = exercise
            // Update our livedata
            exerciseLiveData.value = exercise
        }

    }

    fun completeSet(exerciseLiveData: MutableLiveData<ExerciseEntity>) {
        exerciseLiveData.value?.let { exercise ->
            if (exercise.isTimedExercise) {
                completeSetTimed(exercise)
            } else {
                completeSetReps(exercise)
            }

            // Tell our activity our set was completed, this allows it to handle the notifications
            _setCompletionEvent.value = exercise
            // Update our livedata
            exerciseLiveData.value = exercise
        }
    }

    private fun completeSetTimed(exercise: ExerciseEntity) {
        when {
            exercise.setTimedState == ExerciseSetState.NOT_STARTED -> {
                exercise.setTimedState = ExerciseSetState.PASSED
                passExercise(exercise)
            }
            else -> {
                initializeExerciseEntity(exercise)
            }
        }
    }

    private fun completeSetReps(exercise: ExerciseEntity) {
        exercise.nextSetRestTime = mWaitTimeRegular
        when {
            exercise.set1State == ExerciseSetState.NOT_STARTED -> {
                exercise.set1State = ExerciseSetState.PASSED
                exercise.exerMessage = "Congratulations. Please rest for ${exercise.nextSetRestTime} seconds."
            }
            exercise.set2State == ExerciseSetState.NOT_STARTED -> {
                exercise.set2State = ExerciseSetState.PASSED
                exercise.exerMessage = "Congratulations. Please rest for ${exercise.nextSetRestTime} seconds."

            }
            exercise.set3State == ExerciseSetState.NOT_STARTED -> {
                exercise.set3State = ExerciseSetState.PASSED
                if (exercise.anyFailedExercise()) failExercise(exercise) else passExercise(exercise)
            }
            else -> {
                initializeExerciseEntity(exercise)
            }
        }
    }

    fun completeWorkout() {
        // Update our exercise with our results
        mExerciseList.forEach { it ->
            it.value?.let {

                if (it.anyFailedExercise()) {
                    failExercise(it)
                }
                // Pass any exercise that user was too lazy to mark as passed
                else if (it.isSetNotStarted()) {
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
}