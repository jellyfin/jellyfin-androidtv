package org.jellyfin.androidtv.util.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import org.jellyfin.androidtv.util.locale
import timber.log.Timber

@Composable
@Stable
fun rememberSpeechRecognizerAvailability(context: Context = LocalContext.current): Boolean {
	return remember(context) { SpeechRecognizer.isRecognitionAvailable(context) }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberSpeechRecognizer(
	/**
	 * Invoked when voice input completes successfully with the spoken query.
	 */
	onResult: (String) -> Unit,

	/**
	 * Invoked during voice input with the incomplete spoken query.
	 */
	onPartialResult: ((String) -> Unit)? = null,

	/**
	 * Invoked after calling [SpeechRecognizerState.startListening] if an error status is triggered. Not invoked for immediate statusses
	 * (e.g. unavailable on create)
	 */
	onError: ((status: SpeechRecognizerStatus) -> Unit)? = null,
): SpeechRecognizerState {
	val context = LocalContext.current
	val available = rememberSpeechRecognizerAvailability(context)

	var status by remember { mutableStateOf<SpeechRecognizerStatus>(SpeechRecognizerStatus.Idle) }
	if (!available) status = SpeechRecognizerStatus.Unavailable

	val speechRecognizer = remember(context) { SpeechRecognizer.createSpeechRecognizer(context) }

	fun emitError(errorStatus: SpeechRecognizerStatus) {
		Timber.w("Emitting error $errorStatus")
		status = errorStatus
		onError?.invoke(errorStatus)
	}

	DisposableEffect(speechRecognizer) {
		val listener = object : RecognitionListener {
			override fun onReadyForSpeech(params: Bundle?) {
				status = SpeechRecognizerStatus.Listening(false)
			}

			override fun onBeginningOfSpeech() {
				status = SpeechRecognizerStatus.Listening(true)
			}

			override fun onPartialResults(partialResults: Bundle?) {
				if (onPartialResult == null) return
				val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
				matches?.firstOrNull()?.let(onPartialResult)
			}

			override fun onResults(results: Bundle?) {
				status = SpeechRecognizerStatus.Idle
				val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
				matches?.firstOrNull()?.let(onResult)
			}

			override fun onError(error: Int) {
				when (error) {
					SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> status = SpeechRecognizerStatus.PermissionDenied(true)

					SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
					SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> emitError(SpeechRecognizerStatus.Unavailable)

					SpeechRecognizer.ERROR_NO_MATCH,
					SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> status = SpeechRecognizerStatus.Idle

					else -> emitError(SpeechRecognizerStatus.Error(error))
				}
			}

			// Unused callbacks

			override fun onBufferReceived(buffer: ByteArray?) {}
			override fun onEndOfSpeech() {}
			override fun onEvent(eventType: Int, params: Bundle?) {}
			override fun onRmsChanged(rmsdB: Float) {}
		}

		speechRecognizer.setRecognitionListener(listener)

		onDispose {
			speechRecognizer.destroy()
		}
	}

	fun startListening() {
		val language = context.locale.toLanguageTag()
		Timber.i("Starting to listen for $language speech")

		val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
			putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
			putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, onPartialResult != null)
			putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
		}
		speechRecognizer.startListening(intent)
	}

	fun stopListening() {
		if (status !is SpeechRecognizerStatus.Listening) return

		Timber.i("Stopping speech recognition")
		speechRecognizer.stopListening()
	}

	val recordAudioPermission = rememberPermissionState(
		permission = android.Manifest.permission.RECORD_AUDIO,
		onPermissionResult = { granted ->
			// Start recording as we've just been granted recording permission as result of our request
			if (granted) startListening()
			else status = SpeechRecognizerStatus.PermissionDenied(true)
		}
	)
	var hasRequestedPermission by remember { mutableStateOf(false) }

	fun tryStartListening() {
		// Dont attempt to start when unavailable
		if (!available) return

		when (val permissionStatus = recordAudioPermission.status) {
			is PermissionStatus.Granted -> startListening()

			is PermissionStatus.Denied -> {
				if (permissionStatus.shouldShowRationale || !hasRequestedPermission) {
					status = SpeechRecognizerStatus.RequestingPermission
					hasRequestedPermission = true
					recordAudioPermission.launchPermissionRequest()
				} else {
					emitError(SpeechRecognizerStatus.PermissionDenied(false))
				}
			}
		}
	}

	// Cancel speech when back pressed
	BackHandler(
		enabled = status is SpeechRecognizerStatus.Listening,
		onBack = ::stopListening,
	)

	return SpeechRecognizerState(
		status = status,
		startListening = ::tryStartListening,
		stopListening = ::stopListening,
	)
}
