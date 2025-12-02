package org.jellyfin.androidtv.ui.base.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme

@Composable
fun RadioButton(
	checked: Boolean,
	modifier: Modifier = Modifier,
	shape: Shape = CircleShape,
	containerColor: Color = JellyfinTheme.colorScheme.button,
	contentColor: Color = JellyfinTheme.colorScheme.onButton
) {
	Box(
		modifier = modifier
			.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
			.background(if (checked) containerColor else Color.Unspecified, shape)
			.border(if (checked) 0.dp else 2.dp, containerColor, shape),
		contentAlignment = Alignment.Center,
	) {
		AnimatedVisibility(
			visible = checked,
			modifier = Modifier
				.matchParentSize()
		) {
			Icon(
				painterResource(R.drawable.ic_check),
				tint = contentColor,
				contentDescription = null,
				modifier = Modifier
					.padding(3.dp)
			)
		}
	}
}
