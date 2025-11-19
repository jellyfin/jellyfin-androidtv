package org.jellyfin.androidtv.ui.browsing.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text

@Composable
fun BrowseGridAlphabetPanel(
	selectedLetter: String?,
	onLetterSelected: (String?) -> Unit,
	modifier: Modifier = Modifier
) {
	val letters = remember {
		listOf("#") + ('A'..'Z').map { it.toString() }
	}

	Column(
		modifier = modifier
			.focusGroup()
			.focusRestorer()
			.width(40.dp)
			.fillMaxHeight()
			.padding(vertical = 8.dp),
		horizontalAlignment = Alignment.Start,
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		letters.forEach { letter ->
			AlphabetItem(
				letter = letter,
				isSelected = letter == selectedLetter,
				onClick = { onLetterSelected(letter) }
			)
		}
	}
}

@Composable
private fun AlphabetItem(
	letter: String,
	isSelected: Boolean,
	onClick: () -> Unit
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isFocused by interactionSource.collectIsFocusedAsState()

	Box(
		modifier = Modifier
			.size(11.dp)
			.background(
				when {
					isFocused -> JellyfinTheme.colorScheme.accent
					else -> Color.Transparent
				},
				RoundedCornerShape(2.dp)
			)
			.clickable(
				interactionSource = interactionSource,
				indication = null,
				onClick = onClick
			)
			.focusable(
				interactionSource = interactionSource
			),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = letter,
			color = when {
				isSelected -> Color.White
				isFocused -> Color.White
				else -> Color.Gray
			},
			fontSize = 10.sp,
			fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
		)
	}
}
