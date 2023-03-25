package org.jellyfin.androidtv.ui.preference

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R

private val FontFamily.Companion.SansSerifCondensed by lazy {
	FontFamily(Font(DeviceFontFamilyName("sans-serif-condensed")))
}

private val titleTextStyle = TextStyle(
	fontSize = 14.sp,
)

private val descriptionTextStyle = TextStyle(
	fontFamily = FontFamily.SansSerifCondensed,
	fontSize = 12.sp,
)

@Composable
internal fun PreferenceItem(
	modifier: Modifier = Modifier,
	@DrawableRes iconRes: Int,
	title: String,
	description: String? = null,
	onClick: () -> Unit,
) {
	Row(
		modifier = modifier
			.fillMaxWidth()
			.heightIn(min = 64.dp)
			.clickable(onClick = onClick)
			.focusable(),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			modifier = Modifier
				.padding(start = 24.dp, end = 16.dp)
				.padding(vertical = 16.dp),
			painter = painterResource(id = iconRes),
			contentDescription = null,
			tint = Color.White,
		)
		Column(
			modifier = Modifier,
			verticalArrangement = Arrangement.Center,
		) {
			Text(
				text = title,
				style = titleTextStyle,
				color = Color.White,
			)
			if (description != null) {
				Text(
					modifier = Modifier
						.padding(top = 2.dp),
					text = description,
					style = descriptionTextStyle,
					color = Color.White.copy(alpha = 0.6f),
				)
			}
		}
	}
}

@Preview(
	showBackground = true,
	backgroundColor = 0xFF1b1c1e,
)
@Composable
private fun Preview() {
	PreferenceItem(
		iconRes = R.drawable.ic_users,
		title = stringResource(id = R.string.pref_login),
		description = stringResource(id = R.string.pref_login_description),
		onClick = { },
	)
}
