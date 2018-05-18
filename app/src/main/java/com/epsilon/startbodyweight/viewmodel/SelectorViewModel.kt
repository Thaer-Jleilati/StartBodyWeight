package com.epsilon.startbodyweight.viewmodel

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.epsilon.startbodyweight.data.ExerciseEntity

class SelectorViewModel : ViewModel() {
    var mExerciseList = MutableLiveData<ArrayList<ExerciseEntity>>()

    init {
        mExerciseList.value = ArrayList()
    }

}