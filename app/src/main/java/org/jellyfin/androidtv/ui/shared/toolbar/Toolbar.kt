package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.ui.composable.rememberCurrentTime

@Composable
fun Logo(modifier: Modifier = Modifier) {
	Image(
		painter = painterResource(R.drawable.app_logo),
		contentDescription = stringResource(R.string.app_name),
		modifier = modifier,
	)
}

@Composable
fun Toolbar(
	modifier: Modifier = Modifier,
	start: @Composable () -> Unit = { Logo() },
	center: @Composable () -> Unit = {},
	end: @Composable () -> Unit = { ToolbarClock() },
) {
	ToolbarLayout(
		modifier = modifier
			.height(95.dp)
			.overscan(),
		start = start,
		center = center,
		end = end,
	)
}

@Composable
fun ToolbarClock(
	modifier: Modifier = Modifier,
) {
	val currentTime by rememberCurrentTime()
	Text(
		text = currentTime,
		fontSize = 20.sp,
		color = Color.White,
		modifier = modifier,
	)
}

@Composable
fun ToolbarLayout(
	modifier: Modifier = Modifier,
	start: @Composable () -> Unit = {},
	center: @Composable () -> Unit = {},
	end: @Composable () -> Unit = {},
) = SubcomposeLayout(modifier = modifier) { constraints ->
	val sideConstraints = constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth / 3, minHeight = 0)
	val startPlaceables = subcompose("start", content = start).map { it.measure(sideConstraints) }
	val endPlaceables = subcompose("end", content = end).map { it.measure(sideConstraints) }

	val sideWidth = maxOf(
		startPlaceables.maxOfOrNull { it.width } ?: 0,
		endPlaceables.maxOfOrNull { it.width } ?: 0,
	)

	val centerWidth = (constraints.maxWidth - 2 * sideWidth).coerceAtLeast(0)
	val centerPlaceables = subcompose("center", content = center)
		.map { it.measure(constraints.copy(minWidth = 0, maxWidth = centerWidth)) }

	val height = listOf(
		startPlaceables.maxOfOrNull { it.height } ?: 0,
		centerPlaceables.maxOfOrNull { it.height } ?: 0,
		endPlaceables.maxOfOrNull { it.height } ?: 0
	).maxOrNull() ?: 0

	layout(constraints.maxWidth, height) {
		startPlaceables.forEach { it.placeRelative(0, (height - it.height) / 2) }
		centerPlaceables.forEach { it.place((constraints.maxWidth - it.width) / 2, (height - it.height) / 2) }
		endPlaceables.forEach { it.placeRelative(constraints.maxWidth - it.width, (height - it.height) / 2) }
	}
}

@Composable
fun ToolbarButtons(
	modifier: Modifier = Modifier,
	content: @Composable RowScope.() -> Unit,
) {
	Row(
		modifier = modifier
			.focusRestorer()
			.focusGroup(),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		JellyfinTheme(
			colorScheme = JellyfinTheme.colorScheme.copy(
				button = Color.Transparent
			)
		) {
			content()
		}
	}
}
