package com.epsilon.startbodyweight.selectorActivity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.epsilon.startbodyweight.MainActivity
import com.epsilon.startbodyweight.R
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.RoomDB
import com.epsilon.startbodyweight.workoutActivity.WorkoutActivity
import kotlinx.android.synthetic.main.activity_selector.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class SelectorActivity : AppCompatActivity() {
    private val LTAG = SelectorActivity::class.qualifiedName
    private lateinit var mExerciseSelectAdapter: ExerciseSelectAdapter
    private lateinit var mViewModel: SelectorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector)

        mViewModel = ViewModelProviders.of(this).get(SelectorViewModel::class.java)
        mExerciseSelectAdapter = ExerciseSelectAdapter(mViewModel)

        setupRecyclerView()
        loadExerciseList(intent.hasExtra("LOAD_FROM_DB"), resources)
    }

    private fun setupRecyclerView(){
        rv_select_exers.layoutManager = LinearLayoutManager(this)
        rv_select_exers.setHasFixedSize(true)
        rv_select_exers.adapter = mExerciseSelectAdapter
    }

    private fun loadExerciseList(loadFromDB: Boolean, resources: Resources) {
        Log.d(LTAG, "Adding select exercise list views. Load from DB: $loadFromDB")

        val completeExerciseList = ExerData.getExerciseList(resources)
        if (completeExerciseList.isEmpty()) {
            Log.e(LTAG, "Failed to load exercise list from JSON. Exiting.")
            return
        }

        doAsync {
            val myDBExercises = if (loadFromDB) {
                RoomDB.get(weakRef.get())?.Dao()?.getAllMyExercises()
            } else null
            uiThread {
                var triedToLoadFromDBAndFailed = false

                if (loadFromDB) {
                    // DB load was successful, just add our DB exercises to our adapter directly
                    if (myDBExercises != null && myDBExercises.isNotEmpty()) {
                        mViewModel.populateExerciseListFromDB(myDBExercises, completeExerciseList)
                    } else {
                        // Loading from DB fails
                        Log.e(LTAG, "Load exercises from DB fail, resorting to default exers " + myDBExercises.toString())
                        triedToLoadFromDBAndFailed = true
                    }
                }

                if (!loadFromDB || triedToLoadFromDBAndFailed) {
                    // Loading exercises from JSON with defaults
                    mViewModel.populateExerciseListFromJson(completeExerciseList)
                }

                mExerciseSelectAdapter.notifyDataSetChanged() // TODO test if needed?
            }
        }
    }

    fun saveExerciseSelections(v: View) {
        // Save our exercise list to the DB
        val db = RoomDB.get(this)
        doAsync {
            val exerciseListToSave =
                    if (mViewModel.exerciseList.value != null) mViewModel.exerciseList.value!!
                    else ArrayList()
            val rowsAdded = db?.Dao()?.updateAll(exerciseListToSave)
            uiThread {
                if (rowsAdded.orEmpty().size != exerciseListToSave.size) {
                    Log.e(LTAG, "Failed to add selected exercises to Database.")
                }
                if (it.intent.hasExtra("SELECTED_FROM_MAIN_MENU")) {
                    startActivity(Intent(it, MainActivity::class.java))
                } else {
                    startActivity(Intent(it, WorkoutActivity::class.java))
                }
            }
        }
    }
}