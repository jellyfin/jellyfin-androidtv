package org.jellyfin.androidtv.ui.base.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

@Composable
fun ListItemContent(
	modifier: Modifier = Modifier,
	headingContent: @Composable () -> Unit,
	overlineContent: (@Composable () -> Unit)? = null,
	captionContent: (@Composable () -> Unit)? = null,
	leadingContent: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
	headingStyle: TextStyle,
) {
	Row(
		modifier = modifier
			.padding(12.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		leadingContent?.let { content ->
			Box(
				modifier = Modifier
					.sizeIn(minWidth = 24.dp),
				contentAlignment = Alignment.Center,
				content = {
					ProvideTextStyle(LocalTextStyle.current.copy(color = JellyfinTheme.colorScheme.listCaption)) {
						content()
					}
				}
			)
			Spacer(Modifier.width(14.dp))
		}

		Column(
			modifier = Modifier
				.weight(1f),
		) {
			overlineContent?.let { content ->
				ProvideTextStyle(JellyfinTheme.typography.listOverline.copy(color = JellyfinTheme.colorScheme.listOverline)) {
					content()
				}
				Spacer(Modifier.height(2.dp))
			}

			ProvideTextStyle(headingStyle) {
				headingContent()
			}

			captionContent?.let { content ->
				Spacer(Modifier.height(4.dp))
				ProvideTextStyle(JellyfinTheme.typography.listCaption.copy(color = JellyfinTheme.colorScheme.listCaption)) {
					content()
				}
			}
		}

		trailingContent?.let { content ->
			Spacer(Modifier.width(16.dp))

			Box(
				modifier = Modifier
					.sizeIn(minWidth = 24.dp),
				contentAlignment = Alignment.Center,
				content = { content() }
			)
		}
	}
}
