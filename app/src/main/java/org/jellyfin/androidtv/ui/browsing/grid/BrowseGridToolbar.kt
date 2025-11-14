package org.jellyfin.androidtv.ui.browsing.grid

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jellyfin.androidtv.R
import org.jellyfin.sdk.model.api.ItemSortBy

@Composable
fun BrowseGridToolbar(
	modifier: Modifier = Modifier,
	sortOptions: List<SortOption>,
	currentSortBy: ItemSortBy,
	filterFavoritesOnly: Boolean = false,
	filterUnwatchedOnly: Boolean = false,
	showUnwatchedFilter: Boolean = true,
	showLetterJump: Boolean = true,
	allowViewSelection: Boolean = true,
	onSortSelected: (SortOption) -> Unit,
	onUnwatchedToggle: () -> Unit,
	onFavoriteToggle: () -> Unit,
	onLetterJumpClick: () -> Unit,
	onSettingsClick: () -> Unit
) {
	var showSortDialog by remember { mutableStateOf(false) }

	Row(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 8.dp),
		horizontalArrangement = Arrangement.End,
		verticalAlignment = Alignment.CenterVertically
	) {
		// Sort Button
		ToolbarButton(
			iconRes = R.drawable.ic_sort,
			contentDescription = stringResource(R.string.lbl_sort_by),
			onClick = { showSortDialog = true }
		)

		// Unwatched Filter Button
		if (showUnwatchedFilter) {
			ToolbarButton(
				iconRes = R.drawable.ic_unwatch,
				contentDescription = stringResource(R.string.lbl_unwatched),
				isActive = filterUnwatchedOnly,
				onClick = onUnwatchedToggle
			)
		}

		// Favorite Filter Button
		ToolbarButton(
			iconRes = R.drawable.ic_heart,
			contentDescription = stringResource(R.string.lbl_favorite),
			isActive = filterFavoritesOnly,
			onClick = onFavoriteToggle
		)

		// Letter Jump Button
		if (showLetterJump) {
			ToolbarButton(
				iconRes = R.drawable.ic_jump_letter,
				contentDescription = stringResource(R.string.lbl_by_letter),
				onClick = onLetterJumpClick
			)
		}

		// Settings Button
		ToolbarButton(
			iconRes = R.drawable.ic_settings,
			contentDescription = stringResource(R.string.lbl_settings),
			onClick = onSettingsClick
		)
	}

	// Sort Dialog
	if (showSortDialog) {
		SortDialog(
			sortOptions = sortOptions,
			currentSortBy = currentSortBy,
			onDismiss = { showSortDialog = false },
			onSortSelected = { option ->
				onSortSelected(option)
				showSortDialog = false
			}
		)
	}
}

@Composable
private fun ToolbarButton(
	iconRes: Int,
	contentDescription: String,
	isActive: Boolean = false,
	onClick: () -> Unit
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()

	Box(
		modifier = Modifier
			.size(18.dp)
			.background(
				if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
				RoundedCornerShape(4.dp)
			)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				onClick = onClick
			)
			.focusable(interactionSource = interactionSource),
		contentAlignment = Alignment.Center
	) {
		Image(
			painter = painterResource(iconRes),
			contentDescription = contentDescription,
			modifier = Modifier.size(26.dp),
			colorFilter = ColorFilter.tint(
				if (isActive) Color.Blue else Color.White
			)
		)
	}
}

@Composable
private fun SortDialog(
	sortOptions: List<SortOption>,
	currentSortBy: ItemSortBy,
	onDismiss: () -> Unit,
	onSortSelected: (SortOption) -> Unit
) {
	val focusRequester = remember { FocusRequester() }

	Dialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(
			dismissOnBackPress = true,
			dismissOnClickOutside = true,
			usePlatformDefaultWidth = false
		)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.background(Color.Black.copy(alpha = 0.7f))
				.focusRequester(focusRequester),
			contentAlignment = Alignment.Center
		) {
			Column(
				modifier = Modifier
					.width(400.dp)
					.background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
					.padding(24.dp)
			) {
				BasicText(
					text = stringResource(R.string.lbl_sort_by),
					style = TextStyle(
						color = Color.White,
						fontSize = 24.sp
					),
					modifier = Modifier.padding(bottom = 16.dp)
				)
				sortOptions.forEachIndexed { index, option ->
					SortOptionItem(
						option = option,
						isSelected = option.value == currentSortBy,
						onClick = { onSortSelected(option) }
					)
				}
			}
		}
	}
}

@Composable
private fun SortOptionItem(
	option: SortOption,
	isSelected: Boolean,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()

	Row(
		modifier = modifier
			.clickable(
				interactionSource = interactionSource,
				onClick = onClick
			)
			.focusable(interactionSource = interactionSource)
			.background(
				when {
					isFocused -> Color.White.copy(alpha = 0.3f)
					isSelected -> Color.White.copy(alpha = 0.1f)
					else -> Color.Transparent
				},
				RoundedCornerShape(8.dp)
			)
			.padding(horizontal = 16.dp, vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		// RadioButton
		Box(
			modifier = Modifier
				.size(24.dp)
				.border(
					width = 2.dp,
					color = if (isSelected) Color.Blue else Color.White,
					shape = RoundedCornerShape(12.dp)
				),
			contentAlignment = Alignment.Center
		) {
			if (isSelected) {
				Box(
					modifier = Modifier
						.size(12.dp)
						.background(Color.Blue, RoundedCornerShape(6.dp))
				)
			}
		}
		// Option text
		BasicText(
			text = option.name,
			style = TextStyle(
				color = Color.White,
				fontSize = 18.sp
			),
			modifier = Modifier.padding(start = 12.dp)
		)
	}
}
