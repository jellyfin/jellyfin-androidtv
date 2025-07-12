package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import kotlin.random.Random

/**
 *A header composable for the screensaver view that displays the Jellyfin logo
 * and current time.
 *
 * The logo is horizontally offset by a random amount that changes each time
 * the screensaver changes the title being displayed.
 *
 * @param showLogo Whether to display the Jellyfin logo.
 * @param showClock Whether to display the current time.
 * @param contentKey A stable key (usually the current DreamContent or its ID)
 *                   that changes when
 *                   the screensaver switches content. Used to re-randomize the
 *                   logo position.
 * @param fadeMillis Duration of the fade-in/out animation in milliseconds.
 */
@Composable
fun DreamHeader(
	showLogo: Boolean,
	showClock: Boolean,
	contentKey: Any?,
	fadeMillis: Int,
) {
	AnimatedContent(
		// Jellyfin logo will change positions when content changes
		targetState = contentKey,
		transitionSpec = {
			// use the same timing both ways
			fadeIn(tween(fadeMillis)) togetherWith fadeOut(tween(fadeMillis))
		},
		label = "Header cross-fade",
	) { currentKey ->
		// A random position between 0% and 70% of the
		// screen width.
		val randomPad = remember(currentKey) { Random.nextFloat() * 0.7f }

		Row(
			horizontalArrangement = Arrangement.SpaceBetween,
			modifier = Modifier
				.fillMaxWidth()
				.overscan(),
		) {
			// Place the logo at the randomly selected horizontal
			// position.
			Spacer(Modifier.fillMaxWidth(randomPad))

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
}
