package com.lauruspa.life_management.features.tasks

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.lauruspa.life_management.R
import com.lauruspa.life_management.features.tasks.listener.OnNavToTaskCreationListener

class TasksFragment : Fragment(R.layout.fragment_tasks) {
	private var createTaskButton: MaterialButton? = null
	
	private lateinit var onNavToTaskCreationListener: OnNavToTaskCreationListener
	
	override fun onAttach(context: Context) {
		injectDependencies()
		super.onAttach(context)
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		createTaskButton = view.findViewById(R.id.create_task_button)
		initializeCreateTaskButton()
	}
	
	private fun initializeCreateTaskButton() {
		createTaskButton?.setOnClickListener {
			onNavToTaskCreationListener.navigate()
		}
	}
	
	override fun onDestroyView() {
		createTaskButton = null
		super.onDestroyView()
	}
	
	private fun injectDependencies() {
		val dependencies = (context as TasksDependenciesProvider).getTasksDependencies()
		onNavToTaskCreationListener = dependencies.getOnNavToTaskCreationListener()
	}
}