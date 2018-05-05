package com.epsilon.startbodyweight

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.data.RoomDB
import kotlinx.android.synthetic.main.activity_workout.*
import kotlinx.android.synthetic.main.workout_item.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

private class WorkoutItemAdapter(context: Context, exercise: ArrayList<ExerciseEntity>):
        ArrayAdapter<ExerciseEntity>(context, 0, exercise) {

    override fun getView(position: Int, inputView: View?, parent: ViewGroup): View {
        // Get the data item for this position
        val exercise = getItem(position)
        // Reuse if an existing view is already inflated, otherwise inflate the view
        val workoutItemView = inputView ?: LayoutInflater.from(context).inflate(R.layout.workout_item, parent, false)
        workoutItemView.tag = position

        workoutItemView.tv_exer_name.text = exercise.progressionName
        workoutItemView.tv_exer_message.text = exercise.exerMessage

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
    private var mIncludeDips: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        setupPrefs()
        setupChron()
        setupAdapter()
    }

    private fun setupPrefs() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mIncludeDips = sharedPrefs.getBoolean(
                resources.getString(R.string.pref_include_dips_key),
                resources.getBoolean(R.bool.pref_include_dips_default))
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
            val returnedExerciseList = if (mIncludeDips) {
                db?.Dao()?.getAllMyExercises()
            } else {
                db?.Dao()?.getAllMyExercisesExceptDips()
            }
            uiThread {
                if (returnedExerciseList == null || returnedExerciseList.isEmpty()) {
                    Log.e(LTAG, "No workout data found in DB when entering WorkoutActivity.")
                } else {
                    // Initialize Entity's "next" values
                    returnedExerciseList.forEach {
                        it.nextSet1Reps = it.set1Reps
                        it.nextSet2Reps = it.set2Reps
                        it.nextSet3Reps = it.set3Reps
                        it.nextSetTime = it.setTime
                        it.nextProgressionName = it.progressionName
                        it.nextProgressionNumber = it.progressionNumber
                        it.nextNumAttempts = it.numAttempts
                    }

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
        val exercise = mWorkoutItemAdapter.getItem(position)

        passExerciseAndSetMessage(position)
        parent.tv_exer_message.text = exercise.exerMessage
    }

    private fun passExerciseAndSetMessage(position: Int){
        val exercise = mWorkoutItemAdapter.getItem(position)
        exercise.isModified = true

        // Since we passed, reset our number of attempts
        exercise.nextNumAttempts = 0

        val moveToNextExercise = ExerData.incrementExercise(exercise)
        if (moveToNextExercise) {
            exercise.exerMessage = "Congrats. Moving up!"
            ExerData.setNextProgression(resources, exercise)
            // TODO: Support alternate dips / pushups
        } else {
            exercise.exerMessage = if (exercise.isTimedExercise) {
                "Way to go. Next time you'll do ${exercise.nextSetTime} seconds"
            } else {
                "Way to go. Next time you'll do ${exercise.nextSet1Reps} x ${exercise.nextSet2Reps} x ${exercise.nextSet3Reps}"
            }
        }
    }

    fun failExerciseView(v: View){
        val parent = v.parent as ConstraintLayout
        val position = parent.tag as Int
        val exercise = mWorkoutItemAdapter.getItem(position)

        exercise.isModified = true
        exercise.nextNumAttempts = exercise.numAttempts + 1
        exercise.exerMessage = "Failure is essential. Try again next workout."

        if ( exercise.nextNumAttempts >= 2) {
            ExerData.decrementExercise(exercise)

            exercise.nextNumAttempts = 0
            exercise.exerMessage = if (exercise.isTimedExercise) {
                "2nd attempt at exercise. Lowering difficulty to ${exercise.nextSetTime} seconds"
            }  else {
                "2nd attempt at exercise. Lowering difficulty to ${exercise.nextSet1Reps} x ${exercise.nextSet2Reps} x ${exercise.nextSet3Reps}"
            }
            // TODO: New progression too hard? Do the previous one up until 12 reps
        }

        parent.tv_exer_message.text = exercise.exerMessage
    }

    @SuppressLint("NewApi")
    fun completeSet(v: View) {
        Toast.makeText(this, "Completed set", Toast.LENGTH_SHORT).show()


    }

    fun completeWorkout(v: View){
        // Update our exercise with our results
        mExerciseList.forEachIndexed { i, it ->
            // Automatically pass all untouched exercises
            if (!it.isModified){
                passExerciseAndSetMessage(i)
            }

            it.progressionName = it.nextProgressionName
            it.progressionNumber = it.nextProgressionNumber
            it.set1Reps = it.nextSet1Reps
            it.set2Reps = it.nextSet2Reps
            it.set3Reps = it.nextSet3Reps
            it.setTime = it.nextSetTime
            it.numAttempts = it.nextNumAttempts
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
