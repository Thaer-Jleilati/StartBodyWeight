package com.epsilon.startbodyweight.selectorActivity

import android.R
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.widget.ArrayAdapter
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.databinding.ExerciseSelectBinding

class ExerciseSelectViewHolder(private val mDataBinding: ExerciseSelectBinding,
                               private val mLifecycleOwner: LifecycleOwner,
                               private val mViewModel: SelectorViewModel):
        RecyclerView.ViewHolder(mDataBinding.root){

    init {
        // Used to enable proper observation of LiveData
        mDataBinding.setLifecycleOwner(mLifecycleOwner)
    }

    fun bindExerciseSelectView(exerciseEntity: ExerciseEntity) {
        mDataBinding.exerciseEntity = exerciseEntity
        mDataBinding.viewModel = mViewModel
        mDataBinding.spinnerAdapter = ArrayAdapter(mLifecycleOwner as Context,
                R.layout.simple_spinner_dropdown_item,
                exerciseEntity.allProgressions)
        // Forces the bindings to run immediately
        mDataBinding.executePendingBindings()
    }
}