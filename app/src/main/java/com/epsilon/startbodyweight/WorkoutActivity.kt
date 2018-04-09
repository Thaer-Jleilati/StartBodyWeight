package com.epsilon.startbodyweight

import android.content.Context
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
import com.epsilon.startbodyweight.data.MyExercises
import com.epsilon.startbodyweight.data.RoomDB
import kotlinx.android.synthetic.main.activity_workout.*
import kotlinx.android.synthetic.main.workout_item.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

private class WorkoutItemAdapter(context: Context, exercise: ArrayList<MyExercises>):
        ArrayAdapter<MyExercises>(context, 0, exercise) {

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
    private val mExerciseList = ArrayList<MyExercises>()
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

    fun passExercise(v: View){
        val parent = v.parent as ConstraintLayout
        val position = parent.tag as Int
        val exercise = mWorkoutItemAdapter.getItem(position)
        var moveToNextExercise = false

        if (!exercise.isTimedExercise){
            var reps1 = parent.tv_exer_rep_1.text.toString().toInt()
            var reps2 = parent.tv_exer_rep_2.text.toString().toInt()
            var reps3 = parent.tv_exer_rep_3.text.toString().toInt()
            val minReps = arrayOf(reps1, reps2, reps3).min()

            if (reps1 == minReps){
                reps1++
                if (reps1 > 8) {
                    moveToNextExercise = true
                    reps1 = 4
                    reps2 = 4
                    reps3 = 4
                }
            } else if (reps2 == minReps) {
                reps2++
            } else if (reps3 == minReps) {
                reps3++
            }

            exercise.nextSet1Reps = reps1
            exercise.nextSet2Reps = reps2
            exercise.nextSet3Reps = reps3
            parent.tv_exer_message.text = "Way to go. Next time you'll do $reps1 x $reps2 x $reps3"
        } else {
            var time = parent.tv_exer_time.text.toString().toInt()
            time += 5
            if (time > 60){
                moveToNextExercise = true
                time = 30
            }

            exercise.nextSetTime = time
            parent.tv_exer_message.text = "Way to go. Next time you'll do $time seconds"
        }

        // TODO
        /*
        if (moveToNextExercise){
            parent.tv_exer_message.text = "Congrats. Moving up!"
            val exerNum = parent.getTag(R.id.EXERCISE_NUM_TAG).toString()
            val progressionNum = parent.getTag(R.id.PROGRESSION_NUM_TAG) as Int
            val nextProgression = JSONdata.getNextProgression(resources, exerNum, progressionNum)
            parent.setTag(R.id.NEXT_PROGRESSION_TAG, nextProgression)
        }*/
    }
}