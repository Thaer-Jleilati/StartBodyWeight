package com.epsilon.startbodyweight.selectorActivity

import android.R
import android.support.v7.widget.RecyclerView
import android.widget.ArrayAdapter
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.databinding.ExerciseSelectBinding

class ExerciseSelectViewHolder(private val mDataBinding: ExerciseSelectBinding) :
        RecyclerView.ViewHolder(mDataBinding.root){

    fun bindExerciseSelectView(exerciseEntity: ExerciseEntity, index: Int) {
        mDataBinding.index = index
        mDataBinding.spinnerAdapter = ArrayAdapter(mDataBinding.root.context,
                R.layout.simple_spinner_dropdown_item,
                exerciseEntity.allProgressions)
        // Forces the bindings to run immediately
        mDataBinding.executePendingBindings()
    }
}