package org.jellyfin.androidtv.ui.preference

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R

private val categoryTitleTextStyle = TextStyle(
	fontFamily = FontFamily.SansSerif,
	fontSize = 12.sp,
	fontWeight = FontWeight.Medium,
)

@Composable
internal fun PreferenceCategory(
	modifier: Modifier = Modifier,
	title: String,
) {
	Text(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 24.dp, vertical = 12.dp),
		text = title,
		style = categoryTitleTextStyle,
		color = Color(ContextCompat.getColor(LocalContext.current, R.color.default_preference_color_accent)),
	)
}

@Preview(
	showBackground = true,
	backgroundColor = 0xFF1b1c1e,
)
@Composable
private fun Preview() {
	PreferenceCategory(
		title = stringResource(id = R.string.pref_about_title),
	)
}
