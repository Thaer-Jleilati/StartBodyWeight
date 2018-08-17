package com.epsilon.startbodyweight.workoutActivity

import android.arch.lifecycle.LifecycleOwner
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.epsilon.startbodyweight.databinding.WorkoutItemBinding

class WorkoutItemAdapter(private val mViewModel: WorkoutViewModel,
                         private val mLifecycleOwner: LifecycleOwner) :
        RecyclerView.Adapter<WorkoutItemViewHolder>() {

    init {
        // Use this to avoid blinking of list when data is changed
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = WorkoutItemBinding.inflate(layoutInflater, parent, false)
        dataBinding.viewModel = mViewModel
        dataBinding.setLifecycleOwner(mLifecycleOwner)
        return WorkoutItemViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: WorkoutItemViewHolder, index: Int) {
        holder.bindWorkoutItemView(mViewModel.mExerciseList[index])
    }

    override fun getItemCount(): Int {
        return mViewModel.mExerciseList.size
    }

    // Use this to avoid blinking of list when data is changed
    override fun getItemId(position: Int): Long {
        return mViewModel.mExerciseList[position].value?.exerciseNum?.toLong() ?: 0
    }
}
