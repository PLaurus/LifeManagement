package com.lauruspa.life_management.features.tasks

interface TasksDependenciesProvider {
	fun getTasksDependencies(): TasksDependencies
}