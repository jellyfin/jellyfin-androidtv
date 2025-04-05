package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.modifier.childFocusRestorer
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
	content: @Composable BoxScope.() -> Unit,
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.height(95.dp)
			.overscan(),
	) {
		Logo()

		Spacer(Modifier.width(24.dp))

		Box(
			modifier = Modifier
				.fillMaxHeight()
				.weight(1f)
		) {
			content()
		}
		Spacer(Modifier.width(24.dp))

		val currentTime by rememberCurrentTime()
		Text(
			text = currentTime,
			fontSize = 20.sp,
			color = Color.White,
			modifier = Modifier.align(Alignment.CenterVertically)
		)
	}
}

@Composable
fun BoxScope.ToolbarButtons(
	content: @Composable RowScope.() -> Unit,
) {
	Row(
		modifier = Modifier
			.childFocusRestorer()
			.align(Alignment.CenterEnd),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
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
