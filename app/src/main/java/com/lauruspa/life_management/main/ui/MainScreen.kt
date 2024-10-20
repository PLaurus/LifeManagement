package com.lauruspa.life_management.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.lauruspa.life_management.R
import com.lauruspa.life_management.core.ui.theme.LifeManagementTheme

@Composable
internal fun MainScreen(
	onOpenBottomSheetClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Button(onClick = onOpenBottomSheetClick) {
			Text(text = stringResource(R.string.main_open_bottom_sheet_button))
		}
	}
}

@Preview
@Composable
private fun MainScreenPreview() {
	LifeManagementTheme {
		MainScreen(
			onOpenBottomSheetClick = { },
			modifier = Modifier.fillMaxSize()
		)
	}
}