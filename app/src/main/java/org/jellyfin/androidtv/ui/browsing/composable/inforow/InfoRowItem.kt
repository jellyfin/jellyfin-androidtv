package org.jellyfin.androidtv.ui.browsing.composable.inforow

import org.jellyfin.androidtv.R.font
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ProvideTextStyle
import androidx.compose.ui.text.font.FontFamily
import org.jellyfin.androidtv.R

/**
 * A single item in the [BaseItemInfoRow].
 */
@Composable
fun InfoRowItem(
	// Icon options
	icon: Painter? = null,
	contentDescription: String?,
	// Styling
	colors: Pair<Color, Color> = InfoRowColors.Transparent,
	// Content
	content: @Composable RowScope.() -> Unit,
) {
	val (backgroundColor, foregroundColor) = colors

	val modifier = when {
		backgroundColor.alpha > 0f -> Modifier
			.border(1.2.dp, Color.White, RoundedCornerShape(6.dp))
			.padding(horizontal = 6.dp, vertical = 0.dp)
		else -> Modifier
	}

	val customFont = FontFamily(
		Font(font.poppins_semibold, FontWeight.SemiBold)
	)

	ProvideTextStyle(
		value = TextStyle(
			color = Color.White,
			fontSize = 11.sp,
			fontFamily = customFont,
			fontWeight = FontWeight.SemiBold,
		)
	) {
		Row(
			horizontalArrangement = Arrangement.spacedBy(3.dp),
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier.height(18.dp),
		) {
			if (icon != null) {
				Image(
					painter = icon,
					contentDescription = contentDescription,
					modifier = Modifier.size(16.dp),
				)
			}

			content()
		}
	}
}
