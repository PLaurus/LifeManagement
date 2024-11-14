package com.lauruspa.life_management.features.tasks

import com.lauruspa.life_management.features.tasks.listener.OnNavToTaskCreationListener

interface TasksDependencies {
	fun getOnNavToTaskCreationListener(): OnNavToTaskCreationListener
}