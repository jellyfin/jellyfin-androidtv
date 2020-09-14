package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.data.eventhandling.TvApiEventListener
import org.jellyfin.androidtv.ui.playback.PlaybackManager
import org.jellyfin.apiclient.AppInfo
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.android
import org.jellyfin.apiclient.interaction.AndroidDevice
import org.jellyfin.apiclient.logging.AndroidLogger
import org.jellyfin.apiclient.serialization.GsonJsonSerializer
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val appModule = module {
	single { GsonJsonSerializer() }

	single {
		Jellyfin {
			appInfo = AppInfo("Android TV", BuildConfig.VERSION_NAME)
			logger = AndroidLogger()
			android(androidApplication())
		}
	}

	single {
		get<Jellyfin>().createApi(
				device = AndroidDevice.fromContext(androidApplication()),
				eventListener = TvApiEventListener()
		)
	}
}
