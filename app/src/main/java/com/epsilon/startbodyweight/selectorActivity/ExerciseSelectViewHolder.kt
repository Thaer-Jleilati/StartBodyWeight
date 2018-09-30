package com.epsilon.startbodyweight.selectorActivity

import android.arch.lifecycle.MutableLiveData
import android.support.v7.widget.RecyclerView
import android.widget.ArrayAdapter
import com.epsilon.startbodyweight.R
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.databinding.ExerciseSelectBinding

class ExerciseSelectViewHolder(private val mDataBinding: ExerciseSelectBinding) :
        RecyclerView.ViewHolder(mDataBinding.root){

    fun bindExerciseSelectView(exerciseEntity: MutableLiveData<ExerciseEntity>) {
        mDataBinding.exercise = exerciseEntity
        mDataBinding.spinnerAdapter = ArrayAdapter(mDataBinding.root.context,
                R.layout.support_simple_spinner_dropdown_item,
                exerciseEntity.value?.allProgressions)
        // Forces the bindings to run immediately
        mDataBinding.executePendingBindings()
    }
}