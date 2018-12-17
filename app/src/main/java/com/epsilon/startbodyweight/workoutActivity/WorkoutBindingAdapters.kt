package com.epsilon.startbodyweight.workoutActivity

import android.databinding.BindingAdapter
import android.graphics.Color
import android.widget.TextView
import com.epsilon.startbodyweight.data.ExerciseSetState

@BindingAdapter("app:exerciseBackgroundColor")
fun bindExerciseBackgroundColor(textView: TextView, state: ExerciseSetState?) {
    when (state) {
        ExerciseSetState.PASSED -> textView.setBackgroundColor(Color.GREEN)
        ExerciseSetState.FAILED -> textView.setBackgroundColor(Color.RED)
        else -> textView.setBackgroundColor(Color.TRANSPARENT)
    }
}