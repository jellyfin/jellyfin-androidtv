package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.koin.compose.koinInject

@Composable
private fun SplashScreen(){
	val serverRepository = koinInject<ServerRepository>()
	val backgroundService = koinInject<BackgroundService>()
	val currentServer by serverRepository.currentServer.collectAsState()

	LaunchedEffect(currentServer){
		if (currentServer != null) backgroundService.setBackground(currentServer!!)
		else backgroundService.clearBackgrounds()
	}

	AppBackground()

	Box(
		modifier = Modifier
			.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		Image(
			painter = painterResource(R.drawable.app_logo),
			contentDescription = stringResource(R.string.app_name),
			modifier = Modifier
				.width(400.dp)
				.fillMaxHeight(),
		)
	}
}

class SplashFragment : Fragment(){

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		JellyfinTheme {
			SplashScreen()
		}
	}
}
