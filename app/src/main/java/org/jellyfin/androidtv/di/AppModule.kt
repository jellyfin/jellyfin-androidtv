package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.auth.repository.UserRepositoryImpl
import org.jellyfin.androidtv.data.eventhandling.SocketHandler
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.repository.CustomMessageRepository
import org.jellyfin.androidtv.data.repository.CustomMessageRepositoryImpl
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.data.repository.ItemMutationRepositoryImpl
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.data.repository.NotificationsRepositoryImpl
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepositoryImpl
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.navigation.NavigationRepositoryImpl
import org.jellyfin.androidtv.ui.picture.PictureViewerViewModel
import org.jellyfin.androidtv.ui.playback.PlaybackControllerContainer
import org.jellyfin.androidtv.ui.playback.nextup.NextUpViewModel
import org.jellyfin.androidtv.ui.startup.ServerAddViewModel
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.androidtv.ui.startup.UserLoginViewModel
import org.jellyfin.androidtv.util.MarkdownRenderer
import org.jellyfin.androidtv.util.sdk.legacy
import org.jellyfin.apiclient.AppInfo
import org.jellyfin.apiclient.android
import org.jellyfin.apiclient.logging.AndroidLogger
import org.jellyfin.sdk.android.androidDevice
import org.jellyfin.sdk.api.client.ApiClient
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

val appModule = module {
	// New SDK
	single(defaultDeviceInfo) { androidDevice(get()) }
	single {
		createJellyfin {
			context = androidContext()

			// Add client info
			clientInfo = ClientInfo("Android TV", BuildConfig.VERSION_NAME)
			deviceInfo = get(defaultDeviceInfo)

			// Change server version
			minimumServerVersion = ServerRepository.minimumServerVersion
		}
	}

	single {
		// Create an empty API instance, the actual values are set by the SessionRepository
		get<JellyfinSdk>().createApi()
	}

	single { get<ApiClient>().ws() }

	single { SocketHandler(get(), get(), get(), get(), get(), get(), get()) }

	// Old apiclient
	single {
		JellyfinApiClient {
			appInfo = AppInfo("Android TV", BuildConfig.VERSION_NAME)
			logger = AndroidLogger()
			android(androidApplication())
		}
	}

	single {
		get<JellyfinApiClient>().createApi(
			device = get<DeviceInfo>(defaultDeviceInfo).legacy()
		)
	}

	// Non API related
	single { DataRefreshService() }
	single { PlaybackControllerContainer() }

	single<UserRepository> { UserRepositoryImpl() }
	single<UserViewsRepository> { UserViewsRepositoryImpl(get()) }
	single<NotificationsRepository> { NotificationsRepositoryImpl(get(), get(), get()) }
	single<ItemMutationRepository> { ItemMutationRepositoryImpl(get(), get()) }
	single<CustomMessageRepository> { CustomMessageRepositoryImpl() }
	single<NavigationRepository> { NavigationRepositoryImpl(Destinations.home) }

	viewModel { StartupViewModel(get(), get(), get(), get()) }
	viewModel { UserLoginViewModel(get(), get(), get(), get(defaultDeviceInfo)) }
	viewModel { ServerAddViewModel(get()) }
	viewModel { NextUpViewModel(get(), get(), get(), get()) }
	viewModel { PictureViewerViewModel(get()) }

	single { BackgroundService(get(), get(), get(), get()) }

	single { MarkdownRenderer(get()) }
}
