package com.epsilon.startbodyweight.workoutActivity

import android.arch.lifecycle.MutableLiveData
import android.support.v7.widget.RecyclerView
import com.epsilon.startbodyweight.data.ExerciseEntity
import com.epsilon.startbodyweight.databinding.WorkoutItemBinding

class WorkoutItemViewHolder(private val mDataBinding: WorkoutItemBinding) :
        RecyclerView.ViewHolder(mDataBinding.root) {

    fun bindWorkoutItemView(exerciseEntity: MutableLiveData<ExerciseEntity>) {
        mDataBinding.exercise = exerciseEntity
        // Forces the bindings to run immediately
        mDataBinding.executePendingBindings()
    }
}