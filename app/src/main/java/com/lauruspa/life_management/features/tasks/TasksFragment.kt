package com.lauruspa.life_management.features.tasks

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.lauruspa.life_management.R
import com.lauruspa.life_management.main.MainActivity

class TasksFragment : Fragment(R.layout.fragment_tasks) {
	private var createTaskButton: MaterialButton? = null
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		createTaskButton = view.findViewById(R.id.create_task_button)
		initializeCreateTaskButton()
	}
	
	private fun initializeCreateTaskButton() {
		createTaskButton?.setOnClickListener {
			(activity as? MainActivity)?.navToTaskCreationFragment()
		}
	}
	
	override fun onDestroyView() {
		createTaskButton = null
		super.onDestroyView()
	}
}