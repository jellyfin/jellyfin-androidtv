package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import arrow.core.Either
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.koin.androidx.compose.get

@Composable
fun SplashScreen(
	onSwitchServer: () -> Unit,
	sessionRepository: SessionRepository = get()
) {
	val currentSessionState = sessionRepository.currentSession.collectAsState()
	val coroutineScope = rememberCoroutineScope()
	val screenState = when (currentSessionState.value) {
		is Either.Left -> SplashScreenState.ConnectionFailed(10, {
			// TODO: Is this sane or could this then be cancelled mid-restoration?
			coroutineScope.launch {
				sessionRepository.restoreSession()
			}
		}, onSwitchServer)
		is Either.Right -> SplashScreenState.Empty
	}

	SplashScreenContent(screenState)
}

open class SplashScreenState {
	object Empty : SplashScreenState()
	data class ConnectionFailed(
		val nextRetrySeconds: Int,
		val onRetryRequest: () -> Unit,
		val onSwitchServer: () -> Unit
	) : SplashScreenState()
}

class PreviewProvider: PreviewParameterProvider<SplashScreenState> {
	override val values: Sequence<SplashScreenState> = sequenceOf(
		SplashScreenState.Empty,
		SplashScreenState.ConnectionFailed(30, {}, {})
	)

}

@Preview(device = Devices.TABLET)
@Composable
fun SplashScreenContent(
	@PreviewParameter(PreviewProvider::class) state: SplashScreenState
) {
	Surface(
		color = colorResource(id = R.color.not_quite_black),
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier.fillMaxSize(),
		) {
			Image(
				painter = painterResource(R.drawable.app_logo),
				contentDescription = stringResource(R.string.app_name),
				modifier = Modifier.width(400.dp)
			)
			if (state is SplashScreenState.ConnectionFailed) {
				Spacer(modifier = Modifier.height(40.dp))

				ConnectionFailureDisplay(
					modifier = Modifier.width(400.dp),
					state.nextRetrySeconds,
					state.onRetryRequest,
					state.onSwitchServer
				)
			}
		}
	}
}

@Composable
fun ConnectionFailureDisplay(
	modifier: Modifier,
	nextRetrySeconds: Int,
	onRetryRequest: () -> Unit,
	onSwitchServer: () -> Unit
) {
	Column(
		modifier = modifier
	) {
		Row() {
			Image(
				painter = painterResource(id = R.drawable.ic_baseline_error_24),
				contentDescription = "Error icon",
				modifier = Modifier
					.height(48.dp)
					.width(48.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Column(modifier = Modifier.fillMaxWidth()) {
				Text(
					text = "Failed to connect to server",
					color = Color.White,
				)
				Text(
					text = "Reconnecting in $nextRetrySeconds Seconds",
					color = Color.White
				)
			}
		}
		Row() {
			Button(onClick = onRetryRequest) {
				Text(text = "Retry now")
			}

			Button(onClick = onSwitchServer) {
				Text(text = "Switch server")
			}
		}
	}
}

class SplashFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = ComposeView(requireContext()).apply {
		setContent {
			SplashScreen({setFragmentResult("showServerSelection", bundleOf())})
		}
	}
}
