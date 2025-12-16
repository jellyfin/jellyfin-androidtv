package org.jellyfin.androidtv.ui.base

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

fun colorScheme(): ColorScheme = ColorScheme(
	background = Color(0xFF101010),
	onBackground = Color(0xFFFFFFFF),
	button = Color(0xB3747474),
	onButton = Color(0xFFDDDDDD),
	buttonFocused = Color(0xE6CCCCCC),
	onButtonFocused = Color(0xFF444444),
	buttonDisabled = Color(0x33747474),
	onButtonDisabled = Color(0xFF686868),
	buttonActive = Color(0x4DCCCCCC),
	onButtonActive = Color(0xFFDDDDDD),
	input = Color(0xB3747474),
	onInput = Color(0xE6CCCCCC),
	inputFocused = Color(0xE6CCCCCC),
	onInputFocused = Color(0xFFDDDDDD),
	rangeControlBackground = Color(0xCC4B4B4B),
	rangeControlFill = Color(0xFF00A4DC),
	rangeControlKnob = Color(0xFFFFFFFF),
	seekbarBuffer = Color(0x40FFFFFF),
	recording = Color(0xB3FF7474),
	onRecording = Color(0xFFDDDDDD),
	badge = Color(0xFF62676F),
	onBadge = Color(0xFFE8EAED),
	listHeader = Color(0xFFE0E0E0),
	listOverline = Color(0x66FFFFFF),
	listHeadline = Color(0xFFFFFFFF),
	listCaption = Color(0x99FFFFFF),
	listButton = Color(0x00000000),
	listButtonFocused = Color(0xFF36363B),
	listButtonDisabled = Color(0x33747474),
	surface = Color(0xFF212225),
	scrim = Color(0xAB000000),
)

@Immutable
data class ColorScheme(
	val background: Color,
	val onBackground: Color,

	val button: Color,
	val onButton: Color,
	val buttonFocused: Color,
	val onButtonFocused: Color,
	val buttonDisabled: Color,
	val onButtonDisabled: Color,
	val buttonActive: Color,
	val onButtonActive: Color,

	val input: Color,
	val onInput: Color,
	val inputFocused: Color,
	val onInputFocused: Color,

	val rangeControlBackground: Color,
	val rangeControlFill: Color,
	val rangeControlKnob: Color,
	val seekbarBuffer: Color,

	val recording: Color,
	val onRecording: Color,

	val badge: Color,
	val onBadge: Color,

	val listHeader: Color,
	val listOverline: Color,
	val listHeadline: Color,
	val listCaption: Color,
	val listButton: Color,
	val listButtonFocused: Color,
	val listButtonDisabled: Color,

	val surface: Color,
	val scrim: Color,
)

val LocalColorScheme = staticCompositionLocalOf { colorScheme() }
