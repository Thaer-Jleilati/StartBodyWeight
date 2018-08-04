package com.epsilon.startbodyweight

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.data.RoomDB
import com.epsilon.startbodyweight.viewmodel.SelectorViewModel
import kotlinx.android.synthetic.main.activity_selector.*
import kotlinx.android.synthetic.main.exercise_select.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class SelectorActivity : AppCompatActivity() {
    private val LTAG = SelectorActivity::class.qualifiedName
    private lateinit var mExerciseSelectAdapter: ExerciseSelectAdapter
    private lateinit var mViewModel: SelectorViewModel

    inner class ExerciseSelectViewHolder(private val context: Context, private val exerciseSelectView: View) :
            RecyclerView.ViewHolder(exerciseSelectView) {

        fun bindExerciseSelectView(exercise: ExerciseEntity) {
            // Initialize our spinner
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, exercise.allProgressions)
            exerciseSelectView.sp_sel_exer.adapter = spinnerAdapter
            exerciseSelectView.sp_sel_exer.setSelection(exercise.progressionNumber)
            exerciseSelectView.sp_sel_exer.onItemSelectedListener = object: OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
                {
                    exercise.progressionNumber = position
                    exercise.progressionName = (parent as Spinner).selectedItem.toString()
                }
            }

            // Set up our click listeners
            exerciseSelectView.b_sel_increase_reps_small.setOnClickListener {
                incrementSetSmall(it, exercise)
            }
            exerciseSelectView.b_sel_reduce_reps_small.setOnClickListener {
                decrementSetSmall(it, exercise)
            }

            if (exercise.isTimedExercise) {
                // Hide reps, set time
                exerciseSelectView.tv_sel_rep_1.visibility = View.GONE
                exerciseSelectView.tv_sel_rep_2.visibility = View.GONE
                exerciseSelectView.tv_sel_rep_3.visibility = View.GONE
                exerciseSelectView.tv_sel_time.visibility = View.VISIBLE
            } else {
                // Hide time, set reps
                exerciseSelectView.tv_sel_time.visibility = View.GONE
                exerciseSelectView.tv_sel_rep_1.visibility = View.VISIBLE
                exerciseSelectView.tv_sel_rep_2.visibility = View.VISIBLE
                exerciseSelectView.tv_sel_rep_3.visibility = View.VISIBLE
            }
            setRepsInView(exerciseSelectView, exercise)
        }

    }

    inner class ExerciseSelectAdapter(private val context: Context, private val mExerciseList: ArrayList<ExerciseEntity>) :
            RecyclerView.Adapter<ExerciseSelectViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSelectViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val exerciseSelectView = layoutInflater.inflate(R.layout.exercise_select, parent, false)
            return ExerciseSelectViewHolder(context, exerciseSelectView)
        }

        override fun onBindViewHolder(holder: ExerciseSelectViewHolder, index: Int) {
            holder.bindExerciseSelectView(mExerciseList[index])
        }

        override fun getItemCount(): Int {
            return mExerciseList.size
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector)

        mViewModel = ViewModelProviders.of(this).get(SelectorViewModel::class.java)
        //mViewModel.mExerciseList.observe( this, Observer { user -> Log.d("AEAER", user.)})

        setupRecyclerView()
        setupAdapter()
    }

    private fun setupRecyclerView() {
        rv_select_exers.layoutManager = LinearLayoutManager(this)
        rv_select_exers.setHasFixedSize(true)
    }

    private fun setupAdapter() {
        // Set up adapter
        mExerciseSelectAdapter = ExerciseSelectAdapter(this, mViewModel.mExerciseList.value!!)
        populateAdapterWithExercises(intent.hasExtra("LOAD_FROM_DB"), resources)
        rv_select_exers.adapter = mExerciseSelectAdapter
    }

    private fun populateAdapterWithExercises(loadFromDB: Boolean, resources: Resources) {
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

                mExerciseSelectAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun incrementSetSmall(exerciseSelectView: View, exercise: ExerciseEntity) {
        mViewModel.incrementSet(exercise, smallIncrement = true)
        setRepsInView(exerciseSelectView, exercise)
    }

    private fun decrementSetSmall(exerciseSelectView: View, exercise: ExerciseEntity) {
        mViewModel.decrementSet(exercise, smallDecrement = true)
        setRepsInView(exerciseSelectView, exercise)
    }

    fun saveExerciseSelections(v: View) {
        // Save our exercise list to the DB
        val db = RoomDB.get(this)
        doAsync {
            val rowsAdded = db?.Dao()?.updateAll(mViewModel.mExerciseList.value!!)
            uiThread {
                if (rowsAdded.orEmpty().size != mViewModel.mExerciseList.value?.size) {
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

    private fun setRepsInView(exerciseSelectView: View, exercise: ExerciseEntity) {
        if (exercise.isTimedExercise) {
            exerciseSelectView.tv_sel_time.text = exercise.setTime.toString()
        } else {
            exerciseSelectView.tv_sel_rep_1.text = exercise.set1Reps.toString()
            exerciseSelectView.tv_sel_rep_2.text = exercise.set2Reps.toString()
            exerciseSelectView.tv_sel_rep_3.text = exercise.set3Reps.toString()
        }
    }
}