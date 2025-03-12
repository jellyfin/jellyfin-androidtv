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
	recording = Color(0xB3FF7474),
	onRecording = Color(0xFFDDDDDD),
	badge = Color(0xFF62676F),
	onBadge = Color(0xFFE8EAED),
	listHeader = Color(0xFFE0E0E0),
	listOverline = Color(0x66FFFFFF),
	listHeadline = Color(0xFFFFFFFF),
	listCaption = Color(0x99FFFFFF),
	listButton = Color(0x00000000),
	onListButton = Color(0xFFDDDDDD),
	listButtonFocused = Color(0xFF36363B),
	onListButtonFocused = Color(0xFF444444),
	listButtonDisabled = Color(0x33747474),
	onListButtonDisabled = Color(0xFF686868),
	listButtonActive = Color(0xFF36363B),
	onListButtonActive = Color(0xFFDDDDDD),
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

	val recording: Color,
	val onRecording: Color,

	val badge: Color,
	val onBadge: Color,

	val listHeader: Color,
	val listOverline: Color,
	val listHeadline: Color,
	val listCaption: Color,
	val listButton: Color,
	val onListButton: Color,
	val listButtonFocused: Color,
	val onListButtonFocused: Color,
	val listButtonDisabled: Color,
	val onListButtonDisabled: Color,
	val listButtonActive: Color,
	val onListButtonActive: Color,

	val surface: Color,
	val scrim: Color,
)

val LocalColorScheme = staticCompositionLocalOf { colorScheme() }
