package com.epsilon.startbodyweight.data

import android.content.res.Resources
import android.util.Log
import com.epsilon.startbodyweight.R
import com.google.gson.Gson
import kotlin.math.max

data class Exercise(val name: String, val progs: ArrayList<String>)
data class Exercises(val exers: List<Exercise>)

val LTAG = ExerData::class.qualifiedName

class ExerData {
    companion object {
        const val MIN_EXERCISE_TIME = 30
        const val MAX_EXERCISE_TIME = 60
        const val MIN_EXERCISE_REPS = 4
        const val MAX_EXERCISE_REPS = 8

        private var mCompleteExerciseList: List<Exercise>? = null

        // Singleton so we load list only once
        fun getExerciseList(res: Resources): List<Exercise>?{
            if (mCompleteExerciseList == null) {
                synchronized(ExerData::class){
                    mCompleteExerciseList = loadExerciseListFromFile(res)
                }
            }
            return mCompleteExerciseList
        }

        private fun loadExerciseListFromFile(res: Resources) : List<Exercise>?{
            Log.d(LTAG, "Loading JSON file...")
            val jsonString = res.openRawResource(R.raw.exers).bufferedReader().use { it.readText() }
            Log.d(LTAG, "Loaded JSON file.")
            val parsedExers = Gson().fromJson<Exercises>(jsonString, Exercises::class.java)?.exers
            Log.d(LTAG, "Parsed JSON object")
            return parsedExers
        }

        fun setNextProgression(res: Resources, exercise: ExerciseEntity){
            val exerciseList = getExerciseList(res)
            val progs = exerciseList?.get(exercise.exerciseNum)?.progs.orEmpty()

            // Move up in our progession
            if (exercise.progressionNumber + 1 < progs.size) {
                exercise.nextProgressionNumber = exercise.progressionNumber + 1
                exercise.nextProgressionName = progs[exercise.nextProgressionNumber]
            }
            //If we have maxed out our progressions, stay on the final progression
            else {
                exercise.nextProgressionNumber = exercise.progressionNumber
                exercise.nextProgressionName = exercise.progressionName
            }
        }

        fun stayOnCurrentProgression(exercise: ExerciseEntity){
            exercise.nextProgressionNumber = exercise.progressionNumber
            exercise.nextProgressionName = exercise.progressionName
        }

        fun convertToExerciseEntityList(jsonExerciseList: List<Exercise>?): List<ExerciseEntity>{
            return jsonExerciseList.orEmpty().mapIndexed { i, it ->
                ExerciseEntity(it.name, i, "", 0,
                        MIN_EXERCISE_REPS, MIN_EXERCISE_REPS, MIN_EXERCISE_REPS, MIN_EXERCISE_TIME,
                        isTimedExercise(it.name),0, it.progs,
                        0, 0, 0, 0,
                        "", 0, false)
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


        fun incrementExerciseReps(exercise: ExerciseEntity): Boolean {
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

        fun incrementExerciseTime(exercise: ExerciseEntity): Boolean {
            var moveToNextExercise = false
            exercise.nextSetTime = exercise.setTime + 5
            if (exercise.nextSetTime > MAX_EXERCISE_TIME) {
                moveToNextExercise = true
                exercise.nextSetTime = MIN_EXERCISE_TIME
            }
            return moveToNextExercise
        }
    }
}
