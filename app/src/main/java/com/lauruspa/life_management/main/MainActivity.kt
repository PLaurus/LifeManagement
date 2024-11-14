package com.lauruspa.life_management.main

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.lauruspa.life_management.R
import com.lauruspa.life_management.features.task_creation.TaskCreationFragment
import com.lauruspa.life_management.features.tasks.TasksDependencies
import com.lauruspa.life_management.features.tasks.TasksDependenciesProvider
import com.lauruspa.life_management.features.tasks.listener.OnNavToTaskCreationListener
import com.lauruspa.life_management.features.test.BottomSheetWithScrollingCompose

internal class MainActivity : FragmentActivity(R.layout.activity_main), TasksDependenciesProvider {
	
	fun navToTaskCreationFragment() {
		supportFragmentManager.commit {
			replace<TaskCreationFragment>(R.id.fragment_container)
			addToBackStack(null)
		}
	}
	
	fun openBottomSheetWithScrollingCompose() {
		BottomSheetWithScrollingCompose()
			.show(supportFragmentManager, null)
	}
	
	override fun getTasksDependencies(): TasksDependencies = object : TasksDependencies {
		override fun getOnNavToTaskCreationListener(): OnNavToTaskCreationListener {
			return OnNavToTaskCreationListener { navToTaskCreationFragment() }
		}
	}
}