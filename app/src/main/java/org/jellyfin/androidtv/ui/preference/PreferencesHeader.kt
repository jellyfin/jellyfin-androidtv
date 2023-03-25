package org.jellyfin.androidtv.ui.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R

private val headerTextStyle = TextStyle(
	color = Color.White,
	fontSize = 20.sp,
)

@Composable
internal fun PreferencesHeader(
	modifier: Modifier = Modifier,
	text: String,
) {
	Surface(
		modifier = modifier,
		shadowElevation = 2.dp,
		color = Color(0xFF1b1c1e),
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(90.dp)
				.padding(start = 24.dp, bottom = 18.dp),
			contentAlignment = Alignment.BottomStart,
		) {
			Text(
				text = text,
				style = headerTextStyle,
				fontWeight = FontWeight.Bold,
			)
		}
	}
}

@Preview
@Composable
private fun Preview() {
	PreferencesHeader(text = stringResource(id = R.string.settings_title))
}
