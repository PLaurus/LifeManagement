package com.lauruspa.life_management.main

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.lauruspa.life_management.R
import com.lauruspa.life_management.features.task_creation.TaskCreationFragment
import com.lauruspa.life_management.features.test.BottomSheetWithScrollingCompose

internal class MainActivity : FragmentActivity(R.layout.activity_main) {
	
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
}