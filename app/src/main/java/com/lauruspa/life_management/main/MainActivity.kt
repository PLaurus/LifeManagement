package com.lauruspa.life_management.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.lauruspa.life_management.core.ui.theme.LifeManagementTheme
import com.lauruspa.life_management.features.test.BottomSheetWithScrollingCompose
import com.lauruspa.life_management.main.ui.MainScreen

class MainActivity : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			LifeManagementTheme {
				MainScreen(
					onOpenBottomSheetClick = ::openBottomSheetWithScrollingCompose,
					modifier = Modifier.fillMaxSize()
				)
			}
		}
	}
	
	private fun openBottomSheetWithScrollingCompose() {
		BottomSheetWithScrollingCompose()
			.show(supportFragmentManager, null)
	}
}