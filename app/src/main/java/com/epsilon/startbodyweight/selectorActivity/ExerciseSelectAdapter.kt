package com.epsilon.startbodyweight.selectorActivity

import android.arch.lifecycle.LifecycleOwner
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.epsilon.startbodyweight.databinding.ExerciseSelectBinding

class ExerciseSelectAdapter(private val mViewModel: SelectorViewModel) :
        RecyclerView.Adapter<ExerciseSelectViewHolder>() {

    init {
        // Use this to avoid blinking of list when data is changed
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSelectViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val dataBinding = ExerciseSelectBinding.inflate(layoutInflater, parent, false)
        dataBinding.viewModel = mViewModel
        dataBinding.setLifecycleOwner(parent.context as LifecycleOwner)
        return ExerciseSelectViewHolder(dataBinding)
    }

    override fun onBindViewHolder(holder: ExerciseSelectViewHolder, index: Int) {
        holder.bindExerciseSelectView(mViewModel.mExerciseList[index])
    }

    override fun getItemCount(): Int {
        return mViewModel.mExerciseList.size
    }

    // Use this to avoid blinking of list when data is changed
    override fun getItemId(position: Int): Long {
        return mViewModel.mExerciseList[position].value?.exerciseNum?.toLong() ?: 0
    }
}