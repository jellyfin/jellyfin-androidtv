package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.koin.android.ext.android.inject

@Composable
fun SplashScreen(
	splashscreenUrl: String? = null,
) {
	var splashReady by remember(splashscreenUrl) {
		mutableStateOf(false)
	}

	val splashAlpha by animateFloatAsState(
		targetValue = if (splashReady) 1f else 0f,
		animationSpec = tween(durationMillis = 200),
		label = "SplashBackgroundFadeIn",
	)

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(colorResource(R.color.not_quite_black)),
		contentAlignment = Alignment.Center,
	) {
		if (splashscreenUrl != null) {
			AsyncImage(
				model = splashscreenUrl,
				contentDescription = null,
				contentScale = ContentScale.Crop,
				colorFilter = ColorFilter.tint(
					colorResource(R.color.background_filter),
					BlendMode.SrcAtop,
				),
				onSuccess = { splashReady = true },
				onError = { splashReady = true },
				modifier = Modifier
					.fillMaxSize()
					.alpha(splashAlpha),
			)
		}

		Image(
			painter = painterResource(R.drawable.app_logo),
			contentDescription = stringResource(R.string.app_name),
			modifier = Modifier
				.width(400.dp)
				.fillMaxHeight(),
		)
	}
}

class SplashFragment : Fragment() {
	private val serverRepository: ServerRepository by inject()
	private val jellyfin: Jellyfin by inject()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		val currentServer by serverRepository.currentServer.collectAsState()
		val splashscreenUrl = currentServer
			?.takeIf { it.splashscreenEnabled }
			?.let { jellyfin.createApi(baseUrl = it.address).imageApi.getSplashscreenUrl() }

		JellyfinTheme {
			SplashScreen(splashscreenUrl = splashscreenUrl)
		}
	}
}
