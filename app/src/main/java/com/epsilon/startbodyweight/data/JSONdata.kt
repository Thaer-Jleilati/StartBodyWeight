package com.epsilon.startbodyweight.data

import android.content.res.Resources
import android.util.Log
import com.epsilon.startbodyweight.R
import com.google.gson.Gson

data class Exercise(val name: String, val progs: ArrayList<String>)
data class Exercises(val exers: List<Exercise>)

val LTAG = JSONdata::class.qualifiedName

class JSONdata {
    companion object {
        private var completeExerciseList: List<Exercise>? = null

        // Singleton so we load list only once
        fun getExerciseList(res: Resources): List<Exercise>?{
            if (completeExerciseList == null) {
                synchronized(JSONdata::class){
                    completeExerciseList = loadExerciseListFromFile(res)
                }
            }
            return completeExerciseList
        }

        private fun loadExerciseListFromFile(res: Resources) : List<Exercise>?{
            Log.d(LTAG, "Loading JSON file...")
            val jsonString = res.openRawResource(R.raw.exers).bufferedReader().use { it.readText() }
            Log.d(LTAG, "Loaded JSON file.")
            val parsedExers = Gson().fromJson<Exercises>(jsonString, Exercises::class.java)?.exers
            Log.d(LTAG, "Parsed JSON object")
            return parsedExers
        }

        fun getNextProgression(res: Resources, exerNum: Int, progressionNum: Int): String{
            val exerciseList = getExerciseList(res)
            val progs = exerciseList?.get(exerNum)?.progs

            // TODO
            // return progs?.getOrElse(progressionNum + 1, progressionNum)
            // TODO return the next progression name and number so we can save it in our DB

            return "LOL"
        }

        fun isTimedExercise(exercise: String): Boolean {
            return exercise == "Planks"
        }
    }
}
