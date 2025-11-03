package org.jellyfin.androidtv.ui.browsing.composable.inforow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle

/**
 * A single item in the [BaseItemInfoRow].
 */
@Composable
fun InfoRowItem(
	// Icon options
	icon: ImageVector? = null,
	iconTint: Color? = null,
	contentDescription: String?,
	// Styling
	colors: Pair<Color, Color> = InfoRowColors.Transparent,
	// Content
	content: @Composable RowScope.() -> Unit,
) {
	val (backgroundColor, foregroundColor) = colors

	val modifier = when {
		backgroundColor.alpha > 0f -> Modifier
			.background(backgroundColor, RoundedCornerShape(3.dp))
			.padding(horizontal = 5.dp)

		else -> Modifier
	}

	ProvideTextStyle(
		value = TextStyle(
			color = foregroundColor,
			fontSize = if (backgroundColor.alpha > 0f) 12.sp else 16.sp,
			fontWeight = if (backgroundColor.alpha > 0f) FontWeight.W600 else FontWeight.W500,
		)
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(3.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier.fillMaxHeight(),
		) {
			if (icon != null) {
				Icon(
					imageVector = icon,
					contentDescription = contentDescription,
					modifier = Modifier.size(if (backgroundColor.alpha > 0f) 16.dp else 18.dp),
					tint = iconTint ?: LocalTextStyle.current.color,
				)
			}

			content()
		}
	}
}
