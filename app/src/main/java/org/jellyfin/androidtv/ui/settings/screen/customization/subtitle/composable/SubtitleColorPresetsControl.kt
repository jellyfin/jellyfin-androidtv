package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.ui.base.form.ColorSwatch
import org.jellyfin.design.Tokens

@Composable
fun SubtitleColorPresetsControl(
	presets: Collection<Color>,
	value: Color,
	onValueChange: (color: Color) -> Unit,
) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(Tokens.Space.spaceSm, Alignment.CenterHorizontally),
		modifier = Modifier.fillMaxWidth()
	) {
		for (color in presets) {
			val interactionSource = remember { MutableInteractionSource() }
			val focused by interactionSource.collectIsFocusedAsState()
			val scale by animateFloatAsState(if (focused) 1.25f else 1f)

			ColorSwatch(
				color = color,
				modifier = Modifier
					.size(26.dp)
					.scale(scale)
					.clickable(
						interactionSource = interactionSource,
						role = Role.Button,
						indication = null,
						onClick = { onValueChange(color) },
					)
			)
		}
	}
	Spacer(Modifier.height(Tokens.Space.spaceSm))
}
