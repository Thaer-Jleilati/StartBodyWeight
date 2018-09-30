package com.epsilon.startbodyweight.data

import android.content.res.Resources
import android.util.Log
import com.epsilon.startbodyweight.R
import com.google.gson.Gson
import kotlin.math.max

data class Exercise(val name: String, val exerciseNum: Int, val progs: ArrayList<String>)
data class Exercises(val exers: List<Exercise>)

val LTAG = ExerData::class.qualifiedName

class ExerData {
    companion object {
        const val MIN_EXERCISE_TIME = 30
        const val MAX_EXERCISE_TIME = 60
        const val MIN_EXERCISE_REPS = 4
        const val MAX_EXERCISE_REPS = 8

        private var mCompleteExerciseList: Map<Int, Exercise>? = null

        // Singleton so we load list only once
        fun getExerciseList(res: Resources): Map<Int, Exercise> {
            if (mCompleteExerciseList == null) {
                synchronized(ExerData::class){
                    mCompleteExerciseList = loadExerciseListFromFile(res).orEmpty().map { it.exerciseNum to it }.toMap()
                }
            }
            return mCompleteExerciseList.orEmpty()
        }

        private fun loadExerciseListFromFile(res: Resources) : List<Exercise>?{
            Log.d(LTAG, "Loading JSON file...")
            val jsonString = res.openRawResource(R.raw.exers).bufferedReader().use { it.readText() }
            Log.d(LTAG, "Loaded JSON file.")
            val parsedExers = Gson().fromJson<Exercises>(jsonString, Exercises::class.java)?.exers
            Log.d(LTAG, "Parsed JSON object")
            return parsedExers
        }

        fun convertToExerciseEntityList(jsonExerciseList: Map<Int, Exercise>): List<ExerciseEntity> {
            var sortedExercisesByNum = jsonExerciseList.toSortedMap().values
            return sortedExercisesByNum.map {
                if (it.progs.size == 0) Log.w(LTAG, "Failure in parsing progressions from JSON.")

                var exerciseEntity = ExerciseEntity()
                exerciseEntity.exerciseName = it.name
                exerciseEntity.exerciseNum = it.exerciseNum
                exerciseEntity.set1Reps = MIN_EXERCISE_REPS
                exerciseEntity.set2Reps = MIN_EXERCISE_REPS
                exerciseEntity.set3Reps = MIN_EXERCISE_REPS
                exerciseEntity.setTime = MIN_EXERCISE_TIME
                exerciseEntity.isTimedExercise = isTimedExercise(it.name)
                exerciseEntity.allProgressions = it.progs
                exerciseEntity.progressionNumber = 0
                exerciseEntity.progressionName = it.progs[0]

                exerciseEntity
            }
        }

        private fun isTimedExercise(exercise: String): Boolean {
            return exercise == "Planks"
        }

        fun exceededMaxReps(set1Reps: Int, set2Reps: Int, set3Reps: Int): Boolean{
            return set1Reps > MAX_EXERCISE_REPS || set2Reps > MAX_EXERCISE_REPS || set3Reps > MAX_EXERCISE_REPS
        }

        fun deceededMinReps(set1Reps: Int, set2Reps: Int, set3Reps: Int): Boolean{
            return set1Reps < MIN_EXERCISE_REPS || set2Reps < MIN_EXERCISE_REPS || set3Reps < MIN_EXERCISE_REPS
        }

        fun computeSmallExerciseIncrements(set1Reps: Int, set2Reps: Int, set3Reps: Int): Triple <Int, Int, Int>{
            val minReps = arrayOf(set1Reps, set2Reps, set3Reps).min()
            var nextSet1Reps = set1Reps
            var nextSet2Reps = set2Reps
            var nextSet3Reps = set3Reps

            when {
                set1Reps == minReps -> nextSet1Reps = set1Reps + 1
                set2Reps == minReps -> nextSet2Reps = set2Reps + 1
                set3Reps == minReps -> nextSet3Reps = set3Reps + 1
            }

            return Triple(nextSet1Reps, nextSet2Reps, nextSet3Reps)
        }

        fun computeBigExerciseIncrements(set1Reps: Int, set2Reps: Int, set3Reps: Int): Triple <Int, Int, Int>{
            val max = arrayOf(set1Reps, set2Reps, set3Reps).max() !!
            val min = arrayOf(set1Reps, set2Reps, set3Reps).min()

            // Increment until the numbers are all equal, then increment them all by one
            return if (max == min)
                Triple(set1Reps + 1, set2Reps + 1, set3Reps + 1)
            else
                Triple(max, max, max)
        }

        fun computeSmallExerciseDecrements(set1Reps: Int, set2Reps: Int, set3Reps: Int): Triple <Int, Int, Int>{
            val maxReps = arrayOf(set1Reps, set2Reps, set3Reps).max()
            var nextSet1Reps = set1Reps
            var nextSet2Reps = set2Reps
            var nextSet3Reps = set3Reps

            when {
                set3Reps == maxReps -> nextSet3Reps = set3Reps - 1
                set2Reps == maxReps -> nextSet2Reps = set2Reps - 1
                set1Reps == maxReps -> nextSet1Reps = set1Reps - 1
            }

            return Triple(nextSet1Reps, nextSet2Reps, nextSet3Reps)
        }

        fun computeBigExerciseDecrements(set1Reps: Int, set2Reps: Int, set3Reps: Int): Triple <Int, Int, Int> {
            val max = arrayOf(set1Reps, set2Reps, set3Reps).max()
            val min = arrayOf(set1Reps, set2Reps, set3Reps).min()!!

            // Decrement until the numbers are all equal, then decrement them all by one
            return if (max == min)
                Triple(set1Reps - 1, set2Reps - 1, set3Reps - 1)
            else
                Triple(min, min, min)
        }


        private fun incrementExerciseReps(exercise: ExerciseEntity): Boolean {
            var moveToNextExercise = false

            val (nextSet1Reps, nextSet2Reps, nextSet3Reps) =
                    computeSmallExerciseIncrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)

            if (exceededMaxReps(nextSet1Reps, nextSet2Reps, nextSet3Reps)) {
                moveToNextExercise = true
                exercise.nextSet1Reps = MIN_EXERCISE_REPS
                exercise.nextSet2Reps = MIN_EXERCISE_REPS
                exercise.nextSet3Reps = MIN_EXERCISE_REPS
            } else {
                exercise.nextSet1Reps = nextSet1Reps
                exercise.nextSet2Reps = nextSet2Reps
                exercise.nextSet3Reps = nextSet3Reps
            }

            return moveToNextExercise
        }

        private fun incrementExerciseTime(exercise: ExerciseEntity): Boolean {
            var moveToNextExercise = false

            exercise.nextSetTime = exercise.setTime + 5
            if (exercise.nextSetTime > MAX_EXERCISE_TIME) {
                moveToNextExercise = true
                exercise.nextSetTime = MIN_EXERCISE_TIME
            }
            return moveToNextExercise
        }

        fun incrementExercise(exercise: ExerciseEntity): Boolean {
            return if (exercise.isTimedExercise) {
                incrementExerciseTime(exercise)
            } else {
                incrementExerciseReps(exercise)
            }
        }

        private fun decrementExerciseReps(exercise: ExerciseEntity) {
            exercise.nextSet1Reps = max(exercise.set1Reps - 1, MIN_EXERCISE_REPS)
            exercise.nextSet2Reps = max(exercise.set2Reps - 1, MIN_EXERCISE_REPS)
            exercise.nextSet3Reps = max(exercise.set3Reps - 1, MIN_EXERCISE_REPS)
        }

        private fun decrementExerciseTime(exercise: ExerciseEntity) {
            exercise.nextSetTime = max(exercise.setTime - 10, MIN_EXERCISE_TIME)
        }

        fun decrementExercise(exercise: ExerciseEntity) {
            if (exercise.isTimedExercise) {
                decrementExerciseTime(exercise)
            } else {
                decrementExerciseReps(exercise)
            }
        }
    }
}
