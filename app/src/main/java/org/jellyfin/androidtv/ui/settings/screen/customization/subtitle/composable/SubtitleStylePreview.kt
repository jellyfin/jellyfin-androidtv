package org.jellyfin.androidtv.ui.settings.screen.customization.subtitle.composable

import android.graphics.Typeface
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.TypefaceCompat
import androidx.core.graphics.alpha
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.design.Tokens

@OptIn(UnstableApi::class)
@Composable
fun SubtitleStylePreview(
	userPreferences: UserPreferences,
	subtitlesBackgroundColor: Long = userPreferences[UserPreferences.subtitlesBackgroundColor],
	subtitlesTextWeight: Int = userPreferences[UserPreferences.subtitlesTextWeight],
	subtitlesTextColor: Long = userPreferences[UserPreferences.subtitlesTextColor],
	subtitleTextStrokeColor: Long = userPreferences[UserPreferences.subtitleTextStrokeColor],
	subtitlesTextSize: Float = userPreferences[UserPreferences.subtitlesTextSize],
) {
	val context = LocalContext.current
	SubtitleStylePreview(
		text = stringResource(R.string.subtitle_preview_text),
		textColor = subtitlesTextColor.toInt(),
		textSize = subtitlesTextSize,
		backgroundColor = subtitlesBackgroundColor.toInt(),
		edgeType = if (subtitleTextStrokeColor.toInt().alpha > 0) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
		edgeColor = subtitleTextStrokeColor.toInt(),
		typeface = TypefaceCompat.create(context, Typeface.DEFAULT, subtitlesTextWeight, false),
		modifier = Modifier
			.background(Tokens.Color.colorBluegrey800, JellyfinTheme.shapes.large)
			.fillMaxWidth()
			.height(75.dp)
			.clip(JellyfinTheme.shapes.large)
	)
	Spacer(Modifier.height(Tokens.Space.spaceSm))
}

@OptIn(UnstableApi::class)
@Composable
fun SubtitleStylePreview(
	modifier: Modifier = Modifier,
	text: String = stringResource(R.string.app_name),
	textSize: Float = 12f,
	textColor: Int = CaptionStyleCompat.DEFAULT.foregroundColor,
	backgroundColor: Int = CaptionStyleCompat.DEFAULT.backgroundColor,
	edgeType: Int = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
	edgeColor: Int = CaptionStyleCompat.DEFAULT.edgeColor,
	typeface: Typeface? = CaptionStyleCompat.DEFAULT.typeface,
) = AndroidView(
	modifier = modifier,
	factory = ::SubtitleView,
	update = { view ->
		view.setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)

		view.setStyle(
			CaptionStyleCompat(
				textColor,
				backgroundColor,
				0,
				edgeType,
				edgeColor,
				typeface
			)
		)

		val cue = Cue.Builder().apply {
			setText(text)
			setLine(0.5f, Cue.LINE_TYPE_FRACTION)
			setLineAnchor(Cue.ANCHOR_TYPE_MIDDLE)
		}.build()

		view.setCues(listOf(cue))
	}
)
