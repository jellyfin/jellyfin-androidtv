package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.koin.android.ext.android.inject

@Composable
fun SplashScreen(splashscreenUrl: String? = null, showLogo: Boolean = true) {
	Box(
		modifier = Modifier.background(colorResource(id = R.color.not_quite_black)),
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier.fillMaxSize(),
		) {
			if (splashscreenUrl != null) {
				AsyncImage(
					model = splashscreenUrl,
					contentDescription = stringResource(R.string.app_name),
					modifier = Modifier.fillMaxSize(),
					error = painterResource(R.drawable.app_logo)
				)
			} else if (showLogo) {
				Image(
					painter = painterResource(R.drawable.app_logo),
					contentDescription = stringResource(R.string.app_name),
					modifier = Modifier
						.width(400.dp)
						.fillMaxHeight()
				)
			}
		}
	}
}

class SplashFragment : Fragment() {
	private val serverRepository: ServerRepository by inject()
	private val sessionRepository: SessionRepository by inject()
	private val jellyfin: Jellyfin by inject()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val sessionState by sessionRepository.state.collectAsState()
		val currentServer by serverRepository.currentServer.collectAsState()

		val splashscreenUrl = currentServer
			?.takeIf { it.splashscreenEnabled }
			?.let { jellyfin.createApi(baseUrl = it.address).imageApi.getSplashscreenUrl() }

		// Only fall back to the default logo once session restoration has
		// actually resolved, otherwise we flash the logo before swapping
		// to the server splashscreen once it's known.
		val showLogo = sessionState == SessionRepositoryState.READY

		JellyfinTheme {
			SplashScreen(splashscreenUrl = splashscreenUrl, showLogo = showLogo)
		}
	}
}
