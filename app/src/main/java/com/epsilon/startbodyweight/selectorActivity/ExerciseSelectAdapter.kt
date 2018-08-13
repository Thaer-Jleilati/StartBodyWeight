package com.epsilon.startbodyweight.selectorActivity

import android.arch.lifecycle.LifecycleOwner
import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.epsilon.startbodyweight.R
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.databinding.ExerciseSelectBinding

class ExerciseSelectAdapter(private val mViewModel: SelectorViewModel) :
        RecyclerView.Adapter<ExerciseSelectViewHolder>() {
    private lateinit var mExerciseList: ArrayList<ExerciseEntity>

    init {
        // Use this to avoid blinking of list when data is changed
        setHasStableIds(true)

        // Get a reference to our viewmodel's exercise list
        if (mViewModel.exerciseList.value != null ) mExerciseList = mViewModel.exerciseList.value!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSelectViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding: ExerciseSelectBinding = DataBindingUtil.inflate(layoutInflater, R.layout.exercise_select, parent, false)
        dataBinding.viewModel = mViewModel
        dataBinding.setLifecycleOwner(parent.context as LifecycleOwner)
        return ExerciseSelectViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: ExerciseSelectViewHolder, index: Int) {
        holder.bindExerciseSelectView(mExerciseList[index], index)
    }

    override fun getItemCount(): Int {
        return mExerciseList.size
    }

    // Use this to avoid blinking of list when data is changed
    override fun getItemId(position: Int): Long {
        return mExerciseList[position].exerciseNum.toLong()
    }
}