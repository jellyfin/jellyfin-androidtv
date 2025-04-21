package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.ui.composable.rememberCurrentTime

@Composable
fun DreamHeader(
	showLogo: Boolean,
	showClock: Boolean,
) {
	Row(
		horizontalArrangement = Arrangement.SpaceBetween,
		modifier = Modifier
			.fillMaxWidth()
			.overscan(),
	) {
		// Logo
		AnimatedVisibility(
			visible = showLogo,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier.height(41.dp),
		) {
			Image(
				painter = painterResource(R.drawable.app_logo),
				contentDescription = stringResource(R.string.app_name),
			)
		}

		Spacer(
			modifier = Modifier
				.fillMaxWidth(0f)
		)

		// Clock
		AnimatedVisibility(
			visible = showClock,
			enter = fadeIn(),
			exit = fadeOut(),
		) {
			val currentTime by rememberCurrentTime()
			Text(
				text = currentTime,
				style = TextStyle(
					color = Color.White,
					fontSize = 20.sp
				),
			)
		}
	}
}
