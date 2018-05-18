package com.epsilon.startbodyweight

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
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
import android.widget.LinearLayout
import android.widget.Spinner
import com.epsilon.startbodyweight.data.ExerData
import com.epsilon.startbodyweight.data.ExerData.Companion.MAX_EXERCISE_REPS
import com.epsilon.startbodyweight.data.ExerData.Companion.MAX_EXERCISE_TIME
import com.epsilon.startbodyweight.data.ExerData.Companion.MIN_EXERCISE_REPS
import com.epsilon.startbodyweight.data.ExerData.Companion.MIN_EXERCISE_TIME
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.data.RoomDB
import com.epsilon.startbodyweight.viewmodel.SelectorViewModel
import kotlinx.android.synthetic.main.activity_selector.*
import kotlinx.android.synthetic.main.exercise_select.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.math.max
import kotlin.math.min


class SelectorActivity : AppCompatActivity() {
    private val LTAG = SelectorActivity::class.qualifiedName
    private lateinit var mExerciseSelectAdapter: ExerciseSelectAdapter
    private lateinit var mViewModel: SelectorViewModel

    inner class ExerciseSelectViewHolder(private val context: Context, private val exerciseSelectView: View) :
            RecyclerView.ViewHolder(exerciseSelectView) {

        fun bindExerciseSelectView(position: Int, exercise: ExerciseEntity) {
            exerciseSelectView.tag = position // TODO modify this horrificness to use custom click listeners

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

            if (exercise.isTimedExercise) {
                // Hide reps, set time
                exerciseSelectView.tv_sel_rep_1.visibility = View.GONE
                exerciseSelectView.tv_sel_rep_2.visibility = View.GONE
                exerciseSelectView.tv_sel_rep_3.visibility = View.GONE
                exerciseSelectView.tv_sel_time.visibility = View.VISIBLE
                exerciseSelectView.tv_sel_time.text = exercise.setTime.toString()
            } else {
                // Hide time, set reps
                exerciseSelectView.tv_sel_time.visibility = View.GONE
                exerciseSelectView.tv_sel_rep_1.visibility = View.VISIBLE
                exerciseSelectView.tv_sel_rep_2.visibility = View.VISIBLE
                exerciseSelectView.tv_sel_rep_3.visibility = View.VISIBLE
                setRepsInView(exerciseSelectView, exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
            }
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
            holder.bindExerciseSelectView(index, mExerciseList[index])
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

        // Set up recycler view
        setupRecyclerView()

    }

    private fun setupRecyclerView() {
        rv_select_exers.layoutManager = LinearLayoutManager(this)
        rv_select_exers.setHasFixedSize(true)
        mExerciseSelectAdapter = ExerciseSelectAdapter(this, mViewModel.mExerciseList.value!!)
        populateAdapterWithExercises(mExerciseSelectAdapter, intent.hasExtra("LOAD_FROM_DB"))
        rv_select_exers.adapter = mExerciseSelectAdapter
    }

    private fun populateAdapterWithExercises(exerciseSelectAdapter: ExerciseSelectAdapter, loadFromDB: Boolean) {
        Log.d(LTAG, "Adding select exercise list views. Load from DB: " + loadFromDB)

        val completeExerciseList = ExerData.getExerciseList(resources)
        if (completeExerciseList == null || completeExerciseList.isEmpty()) {
            Log.e(LTAG, "Failed to load exercise list from JSON. Exiting.")
            return
        }

        doAsync {
            val myDBExercises = if (loadFromDB) {
                RoomDB.get(weakRef.get())?.Dao()?.getAllMyExercises()
            } else null
            uiThread {
                val loadDBFail = loadFromDB && myDBExercises.orEmpty().isEmpty()
                if (loadDBFail) Log.e(LTAG, "Load exercises from DB fail " + myDBExercises.toString())

                // DB load was successful, just add our DB exercises to our adapter directly
                if (loadFromDB && !loadDBFail) {
                    // Load our progression list for each exercise from the JSON data and add it to our exercise data
                    myDBExercises?.forEach {
                        it.allProgressions = completeExerciseList.getOrNull(it.exerciseNum)?.progs
                    }
                    mViewModel.mExerciseList.value!!.addAll(myDBExercises!!)
                } else {
                    // Loading exercises from JSON with defaults
                    mViewModel.mExerciseList.value!!.addAll(ExerData.convertToExerciseEntityList(completeExerciseList))
                }
                exerciseSelectAdapter.notifyDataSetChanged()
            }
        }
    }

    fun incrementSetSmall(v:View){
        incrementSet(v, smallIncrement = true)
    }

    fun incrementSetBig(v:View){
        incrementSet(v, smallIncrement = false)
    }

    private fun incrementSet(v: View, smallIncrement: Boolean) {
        val exerciseSelectView = v.parent as LinearLayout
        val position = exerciseSelectView.tag as Int
        val exercise = mViewModel.mExerciseList.value!![position]

        if (exercise.isTimedExercise) {
            val timeIncrement = if (smallIncrement) 5 else 10
            exercise.setTime = min(exercise.setTime + timeIncrement, MAX_EXERCISE_TIME)
            exerciseSelectView.tv_sel_time.text = Integer.toString(exercise.setTime)
        } else {
            var (numReps1, numReps2, numReps3) =
                    if (smallIncrement) {
                        ExerData.computeSmallExerciseIncrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    } else {
                        ExerData.computeBigExerciseIncrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    }
            if (ExerData.exceededMaxReps(numReps1, numReps2, numReps3)) {
                numReps1 = MAX_EXERCISE_REPS
                numReps2 = MAX_EXERCISE_REPS
                numReps3 = MAX_EXERCISE_REPS
            }

            updateRepsInList(exercise, numReps1, numReps2, numReps3)
            setRepsInView(exerciseSelectView, numReps1, numReps2, numReps3)
        }
    }

    fun decrementSetSmall(v:View){
        decrementSet(v, smallDecrement = true)
    }

    fun decrementSetBig(v:View){
        decrementSet(v, smallDecrement = false)
    }


    private fun decrementSet(v: View, smallDecrement: Boolean) {
        val exerciseSelectView = v.parent as LinearLayout
        val position = exerciseSelectView.tag as Int
        val exercise = mViewModel.mExerciseList.value!![position]
        if (exercise.isTimedExercise) {
            val timeDecrement = if (smallDecrement) 5 else 10
            exercise.setTime = max(exercise.setTime - timeDecrement, MIN_EXERCISE_TIME)
            exerciseSelectView.tv_sel_time.text = Integer.toString(exercise.setTime)
        } else {
            var (numReps1, numReps2, numReps3) =
                    if (smallDecrement) {
                        ExerData.computeSmallExerciseDecrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    } else {
                        ExerData.computeBigExerciseDecrements(exercise.set1Reps, exercise.set2Reps, exercise.set3Reps)
                    }
            if (ExerData.deceededMinReps(numReps1, numReps2, numReps3)) {
                numReps1 = MIN_EXERCISE_REPS
                numReps2 = MIN_EXERCISE_REPS
                numReps3 = MIN_EXERCISE_REPS
            }

            updateRepsInList(exercise, numReps1, numReps2, numReps3)
            setRepsInView(exerciseSelectView, numReps1, numReps2, numReps3)
        }
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

    private fun updateRepsInList(exercise: ExerciseEntity, reps1: Int, reps2: Int, reps3: Int) {
        exercise.set1Reps = reps1
        exercise.set2Reps = reps2
        exercise.set3Reps = reps3
    }

    private fun setRepsInView(exerciseSelectView: View, reps1: Int, reps2: Int, reps3: Int) {
        exerciseSelectView.tv_sel_rep_1.text = reps1.toString()
        exerciseSelectView.tv_sel_rep_2.text = reps2.toString()
        exerciseSelectView.tv_sel_rep_3.text = reps3.toString()
    }
}