package com.epsilon.startbodyweight.workoutActivity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.Button
import com.epsilon.startbodyweight.MainActivity
import com.epsilon.startbodyweight.R
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.RoomDB
import com.epsilon.startbodyweight.notif.NotificationUtil
import kotlinx.android.synthetic.main.activity_workout.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class WorkoutActivity : AppCompatActivity() {
    private val LTAG = WorkoutActivity::class.qualifiedName
    private lateinit var mWorkoutItemAdapter: WorkoutItemAdapter
    private lateinit var mViewModel: WorkoutViewModel
    private var mWaitTime: Int = 0
    var mTimeElapsedChron = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        mViewModel = ViewModelProviders.of(this).get(WorkoutViewModel::class.java)
        mWorkoutItemAdapter = WorkoutItemAdapter(mViewModel, this)

        mWaitTime = resources.getInteger(R.integer.wait_time_in_seconds)
        mViewModel.mWaitTime = mWaitTime

        setupPrefs()
        setupChron()
        setupRecyclerView()

        loadExerciseList()

        mViewModel.setCompletionEvent.observe(this, Observer {
            it?.let { exercise ->
                val waitTime = resources.getInteger(R.integer.wait_time_in_seconds)

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
            }
        })
    }

    private fun setupPrefs() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        mViewModel.mIncludeDips = sharedPrefs.getBoolean(
                resources.getString(R.string.pref_include_dips_key),
                resources.getBoolean(R.bool.pref_include_dips_default))
    }

    private fun setupRecyclerView() {
        rv_workout_exers.layoutManager = LinearLayoutManager(this)
        rv_workout_exers.setHasFixedSize(true)
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

    private fun loadExerciseList() {
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
                    mWorkoutItemAdapter.notifyDataSetChanged() //TODO necessary?
                }
            }
        }
    }

    fun completeWorkout(v: View){
        mViewModel.completeWorkout()

        val db = RoomDB.get(this)
        doAsync {
            val rowsAdded = db?.Dao()?.updateAll(mViewModel.mExerciseList.map { it -> it.value!! })
            uiThread {
                if (rowsAdded.orEmpty().size != mViewModel.mExerciseList.size) {
                    Log.e(LTAG, "Failed to add updated exercises to Database.")
                }
                startActivity(Intent(it, MainActivity::class.java))
            }
        }
    }
}
