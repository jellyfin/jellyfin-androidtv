package org.jellyfin.androidtv.di

import androidx.work.WorkManager
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.ServerRepositoryImpl
import org.jellyfin.androidtv.data.eventhandling.TvApiEventListener
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepositoryImpl
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.nextup.NextUpViewModel
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.jellyfin.androidtv.util.sdk.legacy
import org.jellyfin.apiclient.AppInfo
import org.jellyfin.apiclient.android
import org.jellyfin.apiclient.logging.AndroidLogger
import org.jellyfin.apiclient.serialization.GsonJsonSerializer
import org.jellyfin.sdk.android.androidDevice
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.jellyfin.apiclient.Jellyfin as JellyfinApiClient
import org.jellyfin.sdk.Jellyfin as JellyfinSdk

val defaultDeviceInfo = named("defaultDeviceInfo")
val userApiClient = named("userApiClient")
val systemApiClient = named("systemApiClient")

val appModule = module {
	// New SDK
	single(defaultDeviceInfo) { androidDevice(get()) }
	single {
		createJellyfin {
			context = androidContext()

			// Add client info
			clientInfo = ClientInfo("Android TV", BuildConfig.VERSION_NAME)
			// FIXME (SDK 1.2.0) deviceInfo = get(defaultDeviceInfo)
		}
	}

	single(userApiClient) {
		// Create an empty API instance, the actual values are set by the SessionRepository
		get<JellyfinSdk>().createApi()
	}

	single(systemApiClient) {
		// Create an empty API instance, the actual values are set by the SessionRepository
		get<JellyfinSdk>().createApi()
	}

	// Old apiclient
	single { GsonJsonSerializer() }

	single {
		JellyfinApiClient {
			appInfo = AppInfo("Android TV", BuildConfig.VERSION_NAME)
			logger = AndroidLogger()
			android(androidApplication())
		}
	}

	single {
		get<JellyfinApiClient>().createApi(
			device = get<DeviceInfo>(defaultDeviceInfo).legacy(),
			eventListener = TvApiEventListener(get(), get())
		)
	}

	// Non API related
	single { MediaManager() }

	single { DataRefreshService() }

	factory { WorkManager.getInstance(androidContext()) }

	single<ServerRepository> { ServerRepositoryImpl(get(), get(), get(), get()) }
	single<UserViewsRepository> { UserViewsRepositoryImpl(get(userApiClient)) }

	viewModel { LoginViewModel(get(), get(), get()) }
	viewModel { NextUpViewModel(get(), get(userApiClient), get(), get()) }

	single { BackgroundService(get(), get(userApiClient), get()) }
}
