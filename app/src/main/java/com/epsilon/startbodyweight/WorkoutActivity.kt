package com.epsilon.startbodyweight

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import com.epsilon.startbodyweight.data.*
import com.epsilon.startbodyweight.data.ExerData.Companion.MAX_EXERCISE_REPS
import com.epsilon.startbodyweight.data.ExerData.Companion.MAX_EXERCISE_TIME
import com.epsilon.startbodyweight.data.ExerData.Companion.MIN_EXERCISE_REPS
import com.epsilon.startbodyweight.data.ExerData.Companion.MIN_EXERCISE_TIME
import kotlinx.android.synthetic.main.activity_workout.*
import kotlinx.android.synthetic.main.workout_item.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.math.max
import kotlin.math.min

private class WorkoutItemAdapter(context: Context, exercise: ArrayList<ExerciseEntity>):
        ArrayAdapter<ExerciseEntity>(context, 0, exercise) {

    override fun getView(position: Int, inputView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        val exercise = getItem(position)
        // Reuse if an existing view is already inflated, otherwise inflate the view
        val workoutItemView = inputView ?: LayoutInflater.from(context).inflate(R.layout.workout_item, parent, false)
        workoutItemView.tag = position

        // Lookup view for data population
        workoutItemView.tv_exer_name.text = exercise.progressionName

        if (!exercise.isTimedExercise) {
            // Not timed exercise: Hide time, set reps
            workoutItemView.tv_exer_time.visibility = View.GONE
            workoutItemView.tv_exer_rep_1.visibility = View.VISIBLE
            workoutItemView.tv_exer_rep_2.visibility = View.VISIBLE
            workoutItemView.tv_exer_rep_3.visibility = View.VISIBLE
            workoutItemView.tv_exer_rep_1.text = Integer.toString(exercise.set1Reps)
            workoutItemView.tv_exer_rep_2.text = Integer.toString(exercise.set2Reps)
            workoutItemView.tv_exer_rep_3.text = Integer.toString(exercise.set3Reps)
        } else {
            // Timed exercise: Hide reps, set time
            workoutItemView.tv_exer_rep_1.visibility = View.GONE
            workoutItemView.tv_exer_rep_2.visibility = View.GONE
            workoutItemView.tv_exer_rep_3.visibility = View.GONE
            workoutItemView.tv_exer_time.visibility = View.VISIBLE
            workoutItemView.tv_exer_time.text = Integer.toString(exercise.setTime)
        }
        // Return the completed view to render on screen
        return workoutItemView
    }
}

class WorkoutActivity : AppCompatActivity() {
    private val LTAG = WorkoutActivity::class.qualifiedName
    private var mTimeElapsedChron = 0L
    private val mExerciseList = ArrayList<ExerciseEntity>()
    private lateinit var mWorkoutItemAdapter: WorkoutItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        setupChron()
        setupAdapter()
    }

    private fun setupAdapter() {
        mWorkoutItemAdapter = WorkoutItemAdapter(this, mExerciseList)
        populateAdapterWithExercisesFromDB(mWorkoutItemAdapter)
        lv_workout_exers.adapter = mWorkoutItemAdapter
    }

    private fun setupChron() {
        cr_chronometer.stop() // Ensure it is stopped at the beginning
    }

    fun chronoButtonClick(v: View) {
        val button = v as Button
        if (button.text.toString() == getString(R.string.b_start)) {
            cr_chronometer.base = SystemClock.elapsedRealtime() - mTimeElapsedChron
            cr_chronometer.start()
            button.text = getString(R.string.b_pause)
        } else if (button.text.toString() == getString(R.string.b_pause)) {
            mTimeElapsedChron = SystemClock.elapsedRealtime() - cr_chronometer.base
            cr_chronometer.stop()
            button.text = getString(R.string.b_start)
        }
    }


    private fun populateAdapterWithExercisesFromDB(workoutItemAdapter: WorkoutItemAdapter) {
        val db = RoomDB.get(this)
        doAsync {
            var returnedExerciseList = db?.Dao()?.getAllMyExercises()
            uiThread {
                if (returnedExerciseList == null || returnedExerciseList.isEmpty()) {
                    Log.e(LTAG, "No workout data found in DB when entering WorkoutActivity.")
                } else {
                    // Populate our workout adapter with our exercise list, which will inflate all the views.
                    workoutItemAdapter.addAll(returnedExerciseList)
                    workoutItemAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    fun passExerciseView(v: View){
        val parent = v.parent as ConstraintLayout
        val position = parent.tag as Int


        val textMessage = passExerciseAndGetMessage(position)
        parent.tv_exer_message.text = textMessage
    }

    private fun passExerciseAndGetMessage(position: Int): String{
        val exercise = mWorkoutItemAdapter.getItem(position)
        var moveToNextExercise = false
        var returnedMessage: String

        exercise.isModified = true

        if (!exercise.isTimedExercise) {
            moveToNextExercise = ExerData.incrementExerciseReps(exercise)
            returnedMessage = "Way to go. Next time you'll do ${exercise.nextSet1Reps} x ${exercise.nextSet2Reps} x ${exercise.nextSet3Reps}"
        } else {
            moveToNextExercise = ExerData.incrementExerciseTime(exercise)
            returnedMessage = "Way to go. Next time you'll do $exercise.nextSetTime seconds"
        }

        if (moveToNextExercise) {
            returnedMessage = "Congrats. Moving up!"
            ExerData.setNextProgression(resources, exercise)
            // TODO: Support alternate dips / pushups
        } else {
            ExerData.stayOnCurrentProgression(exercise)
        }

        return returnedMessage
    }

    fun failExerciseView(v: View){
        val parent = v.parent as ConstraintLayout
        val position = parent.tag as Int
        val exercise = mWorkoutItemAdapter.getItem(position)

        exercise.isModified = true
        exercise.numAttempts++
        parent.tv_exer_message.text = "Failure is essential. Try again next workout."

        if ( exercise.numAttempts >= 2) {
            exercise.numAttempts = 0
            parent.tv_exer_message.text = "2nd attempt at exercise. Lowering difficulty."

            if (!exercise.isTimedExercise) {
                exercise.nextSetTime = max(exercise.setTime - 10, MIN_EXERCISE_TIME)
            } else {
                exercise.nextSet1Reps = max(exercise.set1Reps - 2, MIN_EXERCISE_REPS)
                exercise.nextSet2Reps = max(exercise.set2Reps - 2, MIN_EXERCISE_REPS)
                exercise.nextSet3Reps = max(exercise.set3Reps - 2, MIN_EXERCISE_REPS)
            }

            // TODO: New progression too hard? Do the previous one up until 12 reps
        }
    }

    fun completeWorkout(v: View){
        // Update our exercise with our results
        mExerciseList.forEachIndexed { i, it ->
            // Automatically pass all untouched exercises
            if (!it.isModified){
                passExerciseAndGetMessage(i)
            }

            it.progressionName = it.nextProgressionName
            it.progressionNumber = it.nextProgressionNumber
            it.set1Reps = it.nextSet1Reps
            it.set2Reps = it.nextSet2Reps
            it.set3Reps = it.nextSet3Reps
            it.setTime = it.nextSetTime
        }
        val db = RoomDB.get(this)
        doAsync {
            val rowsAdded = db?.Dao()?.updateAll(mExerciseList)
            uiThread {
                if (rowsAdded.orEmpty().size != mExerciseList.size) {
                    Log.e(LTAG, "Failed to add updated exercises to Database.")
                }
                startActivity(Intent(it, MainActivity::class.java))
            }
        }
    }
}
