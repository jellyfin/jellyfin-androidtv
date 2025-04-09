package org.jellyfin.androidtv.util.speech

import androidx.compose.runtime.Stable

@Stable
sealed interface SpeechRecognizerStatus {
	data object Idle : SpeechRecognizerStatus

	data class Listening(val hasSpeech: Boolean) : SpeechRecognizerStatus

	data object RequestingPermission : SpeechRecognizerStatus
	data class PermissionDenied(val canRequest: Boolean) : SpeechRecognizerStatus

	data object Unavailable : SpeechRecognizerStatus
	data class Error(val code: Int) : SpeechRecognizerStatus
}
