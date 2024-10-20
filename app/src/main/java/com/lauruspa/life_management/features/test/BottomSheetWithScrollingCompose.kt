package com.lauruspa.life_management.features.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lauruspa.life_management.core.ui.theme.LifeManagementTheme

internal class BottomSheetWithScrollingCompose : BottomSheetDialogFragment() {
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return ComposeView(requireContext()).apply {
			setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
			setContent {
				LifeManagementTheme {
					LazyColumn {
						items(
							count = 100,
						) { index ->
							ListItem(
								index = index,
								modifier = Modifier.fillMaxWidth()
							)
						}
					}
				}
			}
		}
	}
}

@Composable
private fun ListItem(
	index: Int,
	modifier: Modifier = Modifier
) {
	Card(modifier) {
		Text(
			text = "Item: $index",
			modifier = Modifier.padding(16.dp)
		)
	}
}