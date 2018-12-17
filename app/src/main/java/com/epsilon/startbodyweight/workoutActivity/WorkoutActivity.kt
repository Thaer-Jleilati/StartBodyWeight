package com.epsilon.startbodyweight.workoutActivity

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.transition.Slide
import android.transition.TransitionManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import com.epsilon.startbodyweight.MainActivity
import com.epsilon.startbodyweight.R
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.RoomDB
import com.epsilon.startbodyweight.notif.NotificationUtil
import kotlinx.android.synthetic.main.activity_workout.*
import kotlinx.android.synthetic.main.notes_popup.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class WorkoutActivity : AppCompatActivity() {
    private val LTAG = WorkoutActivity::class.qualifiedName
    private lateinit var mWorkoutItemAdapter: WorkoutItemAdapter
    private lateinit var mViewModel: WorkoutViewModel
    var mTimeElapsedChron = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.epsilon.startbodyweight.R.layout.activity_workout)

        mViewModel = ViewModelProviders.of(this).get(WorkoutViewModel::class.java)
        mWorkoutItemAdapter = WorkoutItemAdapter(mViewModel)

        mViewModel.mWaitTimeRegular = resources.getInteger(com.epsilon.startbodyweight.R.integer.wait_time_regular_in_seconds)
        mViewModel.mWaitTimeFailed = resources.getInteger(com.epsilon.startbodyweight.R.integer.wait_time_failed_in_seconds)

        setupPrefs()
        setupChron()
        setupRecyclerView()

        loadExerciseList()

        mViewModel.setCompletionEvent.observe(this, Observer {
            it?.let { exercise ->
                // Handle the notifications
                NotificationUtil.clearAllNotifications(this)
                // Cancel notifications if we just completed all the sets
                if (exercise.allSetsAttempted()) {
                    chronoButtonInitialize()
                    NotificationUtil.cancelPendingNotification(this)
                }
                // Otherwise, if we are still working on the exercise, schedule a notification
                else {
                    chronoButtonRestart()
                    NotificationUtil.scheduleNotification(this, "Ready to go", "Perform your next set now.", exercise.nextSetRestTime)
                }
            }
        })

        mViewModel.openNoteEvent.observe(this, Observer {
            it?.let { exercise ->
                // Inflate a custom view using layout inflater
                val notesPopupView = LayoutInflater.from(this).inflate(R.layout.notes_popup, null)

                // Initialize a new instance of popup window
                val popupWindow = PopupWindow(notesPopupView,
                        LinearLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
                        LinearLayout.LayoutParams.WRAP_CONTENT // Window height
                )

                // Set an elevation for the popup window
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    popupWindow.elevation = 10.0F
                }

                // If API level 23 or higher then execute the code
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Create a new slide animation for popup window enter transition
                    val slideIn = Slide()
                    slideIn.slideEdge = Gravity.TOP
                    popupWindow.enterTransition = slideIn

                    // Slide animation for popup window exit transition
                    val slideOut = Slide()
                    slideOut.slideEdge = Gravity.BOTTOM
                    popupWindow.exitTransition = slideOut
                }

                notesPopupView.et_note.setText(exercise.extraNote)
                notesPopupView.b_close.setOnClickListener { popupWindow.dismiss() }

                // Set a dismiss listener for popup window
                popupWindow.setOnDismissListener {
                    exercise.extraNote = popupWindow.contentView.et_note.text.toString()
                    Toast.makeText(applicationContext, "Note saved.", Toast.LENGTH_SHORT).show()
                }

                // Enable focusing on the edit text
                popupWindow.isFocusable = true

                TransitionManager.beginDelayedTransition(rv_workout_exers)
                popupWindow.showAtLocation(rv_workout_exers, Gravity.CENTER, 0, 0)
            }
        })
    }

    private fun setupPrefs() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        /*mViewModel.mIncludeDips = sharedPrefs.getBoolean(
                resources.getString(R.string.pref_include_dips_key),
                resources.getBoolean(R.bool.pref_include_dips_default))*/
    }

    private fun setupRecyclerView() {
        rv_workout_exers.layoutManager = LinearLayoutManager(this)
        rv_workout_exers.setHasFixedSize(true)
        rv_workout_exers.adapter = mWorkoutItemAdapter
    }

    private fun setupChron() {
        chronoButtonInitialize() // Ensure it is stopped at the beginning
    }

    private fun chronoButtonInitialize() {
        cr_chronometer.stop()
        chronoButtonResetTime()
    }

    fun chronoButtonStartPause(v: View) {
        val button = v as Button
        if (button.text.toString() == getString(com.epsilon.startbodyweight.R.string.b_start)) {
            cr_chronometer.base = SystemClock.elapsedRealtime() - mTimeElapsedChron
            cr_chronometer.start()
            button.text = getString(com.epsilon.startbodyweight.R.string.b_pause)
        } else if (button.text.toString() == getString(com.epsilon.startbodyweight.R.string.b_pause)) {
            mTimeElapsedChron = SystemClock.elapsedRealtime() - cr_chronometer.base
            cr_chronometer.stop()
            button.text = getString(com.epsilon.startbodyweight.R.string.b_start)
        }
    }

    fun chronoButtonResetTime(v: View? = null) {
        cr_chronometer.base = SystemClock.elapsedRealtime()
        mTimeElapsedChron = 0
    }

    private fun chronoButtonRestart() {
        chronoButtonResetTime()
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
            val myDBExercises = db?.Dao()?.getAllMyExercises()

            uiThread {
                if (myDBExercises == null || myDBExercises.isEmpty()) {
                    Log.e(LTAG, "No workout data found in DB when entering WorkoutActivity.")
                } else {
                    mViewModel.populateExerciseListFromDB(myDBExercises, completeExerciseList)
                    // We absolutely need this
                    mWorkoutItemAdapter.notifyDataSetChanged()
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
                AlertDialog.Builder(it)
                        .setTitle("Complete workout?")
                        .setMessage("All the exercises not selected as 'FAIL' will be set as completed.")
                        .setIcon(android.R.drawable.checkbox_on_background)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            // End current activity
                            finish()
                            startActivity(Intent(it, MainActivity::class.java))
                        }.show()
            }
        }
    }
}
