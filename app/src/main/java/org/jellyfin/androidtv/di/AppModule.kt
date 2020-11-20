package org.jellyfin.androidtv.di

import androidx.core.content.getSystemService
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.data.eventhandling.TvApiEventListener
import org.jellyfin.androidtv.data.repository.ServerRepository
import org.jellyfin.androidtv.data.repository.ServerRepositoryImpl
import org.jellyfin.androidtv.data.repository.UserRepository
import org.jellyfin.androidtv.data.repository.UserRepositoryImpl
import org.jellyfin.androidtv.data.source.CredentialsFileSource
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.jellyfin.apiclient.AppInfo
import org.jellyfin.apiclient.Jellyfin
import org.jellyfin.apiclient.android
import org.jellyfin.apiclient.interaction.AndroidDevice
import org.jellyfin.apiclient.interaction.device.IDevice
import org.jellyfin.apiclient.logging.AndroidLogger
import org.jellyfin.apiclient.model.apiclient.ServerInfo
import org.jellyfin.apiclient.serialization.GsonJsonSerializer
import org.jellyfin.apiclient.serialization.ServerInfoDeserializer
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
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

	single {
		get<Jellyfin>().createApi(
			device = get(),
			eventListener = TvApiEventListener()
		)
	}

	single(named("CredentialsFileSerializer")) {
		GsonBuilder()
			.setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
			.registerTypeAdapter(ServerInfo::class.java, ServerInfoDeserializer())
			.setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
			.create()
	}

	single {
		CredentialsFileSource(androidApplication(), get(named("CredentialsFileSerializer")))
	}

	single<ServerRepository> {
		ServerRepositoryImpl(get(), get(), get())
	}

	single<UserRepository> {
		UserRepositoryImpl(get(), get(), get(), get(), get())
	}

	viewModel {
		LoginViewModel(get(), get(), get())
	}

	single {AuthenticationStore() }
	single { AuthenticationRepository(androidContext().getSystemService()!!, get())}
}
