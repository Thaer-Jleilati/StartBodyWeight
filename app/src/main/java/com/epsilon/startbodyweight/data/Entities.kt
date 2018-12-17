package com.epsilon.startbodyweight.data


import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey

enum class ExerciseSetState {
    NOT_STARTED, PASSED, FAILED
}

@Entity
class ExerciseEntity(
        var exerciseName: String = "",
        @PrimaryKey var exerciseNum: Int = 0,
        var progressionName: String = "",
        var progressionNumber: Int = 0,
        var set1Reps: Int = 0,
        var set2Reps: Int = 0,
        var set3Reps: Int = 0,
        var setTime: Int = 0,
        var isTimedExercise: Boolean = false,
        var numAttempts: Int = 0,
        var isActive: Boolean = true,
        var extraNote: String = "",

        // Do not store the following items in our DB
        @Ignore var allProgressions: ArrayList<String> = ArrayList(),
        @Ignore var nextSet1Reps: Int = 0,
        @Ignore var nextSet2Reps: Int = 0,
        @Ignore var nextSet3Reps: Int = 0,
        @Ignore var nextSetTime: Int = 0,
        @Ignore var nextNumAttempts: Int = 0,
        @Ignore var nextProgressionName: String = "",
        @Ignore var nextProgressionNumber: Int = 0,
        @Ignore var exerMessage: String = "",
        @Ignore var set1State: ExerciseSetState = ExerciseSetState.NOT_STARTED,
        @Ignore var set2State: ExerciseSetState = ExerciseSetState.NOT_STARTED,
        @Ignore var set3State: ExerciseSetState = ExerciseSetState.NOT_STARTED,
        @Ignore var setTimedState: ExerciseSetState = ExerciseSetState.NOT_STARTED,
        @Ignore var nextSetRestTime: Int = 0
) {
    fun anyFailedExercise(): Boolean {
        return set1State == ExerciseSetState.FAILED ||
                set2State == ExerciseSetState.FAILED ||
                set3State == ExerciseSetState.FAILED
    }

    fun allSetsAttempted(): Boolean {
        return if (isTimedExercise) {
            setTimedState != ExerciseSetState.NOT_STARTED
        } else
            set1State != ExerciseSetState.NOT_STARTED &&
                    set2State != ExerciseSetState.NOT_STARTED &&
                    set3State != ExerciseSetState.NOT_STARTED
    }

    fun isSetNotStarted(): Boolean {
        return if (isTimedExercise) {
            setTimedState == ExerciseSetState.NOT_STARTED
        } else {
            set1State == ExerciseSetState.NOT_STARTED &&
                    set2State == ExerciseSetState.NOT_STARTED &&
                    set3State == ExerciseSetState.NOT_STARTED
        }
    }
}
