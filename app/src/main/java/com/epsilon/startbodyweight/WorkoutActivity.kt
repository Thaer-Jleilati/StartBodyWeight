package com.epsilon.startbodyweight

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.data.RoomDB
import com.epsilon.startbodyweight.notif.NotificationUtil
import com.epsilon.startbodyweight.viewmodel.WorkoutViewModel
import kotlinx.android.synthetic.main.activity_workout.*
import kotlinx.android.synthetic.main.workout_item.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class WorkoutActivity : AppCompatActivity() {
    private val LTAG = WorkoutActivity::class.qualifiedName
    private lateinit var mWorkoutItemAdapter: WorkoutItemAdapter
    private lateinit var mViewModel: WorkoutViewModel
    var mTimeElapsedChron = 0L

    fun styleCompletedSets(workoutItemView: View, exercise: ExerciseEntity) {
        if (exercise.isTimedExercise) {
            workoutItemView.tv_exer_time.setBackgroundColor(if (exercise.isSetTimeComplete) Color.GREEN else Color.TRANSPARENT)
        } else {
            workoutItemView.tv_exer_rep_1.setBackgroundColor(if (exercise.isSet1Complete) Color.GREEN else Color.TRANSPARENT)
            workoutItemView.tv_exer_rep_2.setBackgroundColor(if (exercise.isSet2Complete) Color.GREEN else Color.TRANSPARENT)
            workoutItemView.tv_exer_rep_3.setBackgroundColor(if (exercise.isSet3Complete) Color.GREEN else Color.TRANSPARENT)
        }
    }

    inner class WorkoutItemViewHolder(private val workoutItemView: View) :
            RecyclerView.ViewHolder(workoutItemView) {

        fun bindWorkoutItemView(exercise: ExerciseEntity) {
            workoutItemView.tv_exer_name.text = exercise.progressionName
            workoutItemView.tv_exer_message.text = exercise.exerMessage

            // Set up our click listeners
            workoutItemView.b_exer_fail.setOnClickListener {
                failExerciseView(workoutItemView, exercise)
            }
            workoutItemView.setOnClickListener {
                completeSet(it, exercise)
            }

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

            styleCompletedSets(workoutItemView, exercise)
        }
    }

    inner class WorkoutItemAdapter(private val mExerciseList: ArrayList<ExerciseEntity>) :
            RecyclerView.Adapter<WorkoutItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutItemViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val workoutItemView = layoutInflater.inflate(R.layout.workout_item, parent, false)
            return WorkoutItemViewHolder(workoutItemView)
        }

        override fun onBindViewHolder(holder: WorkoutItemViewHolder, index: Int) {
            holder.bindWorkoutItemView(mExerciseList[index])
        }

        override fun getItemCount(): Int {
            return mExerciseList.size
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        mViewModel = ViewModelProviders.of(this).get(WorkoutViewModel::class.java)

        setupPrefs()
        setupChron()
        setupAdapter()
    }

    private fun setupPrefs() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mViewModel.mIncludeDips = sharedPrefs.getBoolean(
                resources.getString(R.string.pref_include_dips_key),
                resources.getBoolean(R.bool.pref_include_dips_default))
    }

    private fun setupAdapter() {
        mWorkoutItemAdapter = WorkoutItemAdapter(mViewModel.mExerciseList)
        populateAdapterWithExercises(mWorkoutItemAdapter)
        rv_workout_exers.adapter = mWorkoutItemAdapter
    }

    private fun setupChron() {
        cr_chronometer.stop() // Ensure it is stopped at the beginning
    }

    fun chronoButtonStartPause(v: View) {
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

    fun chronoButtonResetTime(v: View?) {
        cr_chronometer.base = SystemClock.elapsedRealtime()
        mTimeElapsedChron = 0
    }

    private fun chronoButtonRestart() {
        chronoButtonResetTime(null)
        cr_chronometer.start()
    }

    private fun populateAdapterWithExercises(workoutItemAdapter: WorkoutItemAdapter) {
        val completeExerciseList = ExerData.getExerciseList(resources)
        if (completeExerciseList.isEmpty()) {
            Log.e(LTAG, "Failed to load exercise list from JSON. Exiting.")
            return
        }

        val db = RoomDB.get(this)
        doAsync {
            val myDBExercises = if (mViewModel.mIncludeDips) {
                db?.Dao()?.getAllMyExercises()
            } else {
                db?.Dao()?.getAllMyExercisesExceptDips()
            }
            uiThread {
                if (myDBExercises == null || myDBExercises.isEmpty()) {
                    Log.e(LTAG, "No workout data found in DB when entering WorkoutActivity.")
                } else {
                    mViewModel.populateExerciseListFromDB(myDBExercises, completeExerciseList)
                    workoutItemAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    fun failExerciseView(parent: View, exercise: ExerciseEntity) {
        mViewModel.failExercise(exercise)
        parent.tv_exer_message.text = exercise.exerMessage
    }

    fun completeSet(workoutItemView: View, exercise: ExerciseEntity) {
        val waitTime = resources.getInteger(R.integer.wait_time_in_seconds)
        mViewModel.completeSet(exercise, waitTime)

        // Handle the notifications
        NotificationUtil.clearAllNotifications(this)
        if (!exercise.isTimedExercise) {
            // Cancel notifications if we just completed all the sets
            if (exercise.isSet1Complete && exercise.isSet2Complete && exercise.isSet3Complete) {
                NotificationUtil.cancelPendingNotification(this)
            }
            // Otherwise, if we are still working on the exercise, schedule a notification
            else if (exercise.isSet1Complete || exercise.isSet2Complete) {
                chronoButtonRestart()
                NotificationUtil.scheduleNotification(this, "Ready to go", "Perform your next set now.", waitTime)
            }
        } else {
            if (exercise.isSetTimeComplete) NotificationUtil.cancelPendingNotification(this)
        }

        styleCompletedSets(workoutItemView, exercise)
        workoutItemView.tv_exer_message.text = exercise.exerMessage
    }

    fun completeWorkout(v: View){
        mViewModel.completeWorkout()

        val db = RoomDB.get(this)
        doAsync {
            val rowsAdded = db?.Dao()?.updateAll(mViewModel.mExerciseList)
            uiThread {
                if (rowsAdded.orEmpty().size != mViewModel.mExerciseList.size) {
                    Log.e(LTAG, "Failed to add updated exercises to Database.")
                }
                startActivity(Intent(it, MainActivity::class.java))
            }
        }
    }
}
