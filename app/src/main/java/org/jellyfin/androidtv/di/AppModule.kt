package org.jellyfin.androidtv.di

import androidx.work.WorkManager
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.ServerRepositoryImpl
import org.jellyfin.androidtv.data.eventhandling.TvApiEventListener
import org.jellyfin.androidtv.data.model.DataRefreshService
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.ui.playback.nextup.NextUpViewModel
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.jellyfin.apiclient.AppInfo
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.android
import org.jellyfin.apiclient.interaction.AndroidDevice
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.logging.AndroidLogger
import org.jellyfin.apiclient.serialization.GsonJsonSerializer
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
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

	single<IDevice> {
		AndroidDevice.fromContext(androidApplication())
	}

	single { MediaManager() }

	single {
		get<Jellyfin>().createApi(
			device = get(),
			eventListener = TvApiEventListener(get(), get())
		)
	}

	single { DataRefreshService() }

	factory { WorkManager.getInstance(androidContext()) }

	single<ServerRepository> { ServerRepositoryImpl(get(), get(), get(), get(), get()) }

	viewModel { LoginViewModel(get(), get()) }
	viewModel { NextUpViewModel(get(), get()) }

	single { BackgroundService(get(), get(), get()) }
}
