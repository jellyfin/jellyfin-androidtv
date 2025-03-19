package org.jellyfin.androidtv.ui.base

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

@Composable
fun Text(
	text: String,
	modifier: Modifier = Modifier,
	color: Color = Color.Unspecified,
	fontSize: TextUnit = TextUnit.Unspecified,
	fontStyle: FontStyle? = null,
	fontWeight: FontWeight? = null,
	fontFamily: FontFamily? = null,
	letterSpacing: TextUnit = TextUnit.Unspecified,
	textDecoration: TextDecoration? = null,
	textAlign: TextAlign? = null,
	lineHeight: TextUnit = TextUnit.Unspecified,
	overflow: TextOverflow = TextOverflow.Clip,
	softWrap: Boolean = true,
	maxLines: Int = Int.MAX_VALUE,
	minLines: Int = 1,
	onTextLayout: (TextLayoutResult) -> Unit = {},
	style: TextStyle = LocalTextStyle.current
) {
	val textColor = color.takeOrElse { style.color.takeOrElse { Color.Black } }

	BasicText(
		text = text,
		modifier = modifier.graphicsLayer { this.compositingStrategy = CompositingStrategy.Offscreen },
		style = style.merge(
			color = textColor,
			fontSize = fontSize,
			fontWeight = fontWeight,
			textAlign = textAlign ?: TextAlign.Unspecified,
			lineHeight = lineHeight,
			fontFamily = fontFamily,
			textDecoration = textDecoration,
			fontStyle = fontStyle,
			letterSpacing = letterSpacing
		),
		onTextLayout = onTextLayout,
		overflow = overflow,
		softWrap = softWrap,
		maxLines = maxLines,
		minLines = minLines
	)
}

@Composable
fun Text(
	text: AnnotatedString,
	modifier: Modifier = Modifier,
	color: Color = Color.Unspecified,
	fontSize: TextUnit = TextUnit.Unspecified,
	fontStyle: FontStyle? = null,
	fontWeight: FontWeight? = null,
	fontFamily: FontFamily? = null,
	letterSpacing: TextUnit = TextUnit.Unspecified,
	textDecoration: TextDecoration? = null,
	textAlign: TextAlign? = null,
	lineHeight: TextUnit = TextUnit.Unspecified,
	overflow: TextOverflow = TextOverflow.Clip,
	softWrap: Boolean = true,
	maxLines: Int = Int.MAX_VALUE,
	minLines: Int = 1,
	inlineContent: Map<String, InlineTextContent> = mapOf(),
	onTextLayout: (TextLayoutResult) -> Unit = {},
	style: TextStyle = LocalTextStyle.current
) {
	val textColor = color.takeOrElse { style.color.takeOrElse { Color.Black } }

	BasicText(
		text = text,
		modifier = modifier.graphicsLayer { this.compositingStrategy = CompositingStrategy.Offscreen },
		style =
			style.merge(
				color = textColor,
				fontSize = fontSize,
				fontWeight = fontWeight,
				textAlign = textAlign ?: TextAlign.Unspecified,
				lineHeight = lineHeight,
				fontFamily = fontFamily,
				textDecoration = textDecoration,
				fontStyle = fontStyle,
				letterSpacing = letterSpacing
			),
		onTextLayout = onTextLayout,
		overflow = overflow,
		softWrap = softWrap,
		maxLines = maxLines,
		minLines = minLines,
		inlineContent = inlineContent
	)
}

val LocalTextStyle = compositionLocalOf(structuralEqualityPolicy()) { TypographyDefaults.Default }

@Composable
fun ProvideTextStyle(value: TextStyle, content: @Composable () -> Unit) {
	val mergedStyle = LocalTextStyle.current.merge(value)
	CompositionLocalProvider(LocalTextStyle provides mergedStyle, content = content)
}
