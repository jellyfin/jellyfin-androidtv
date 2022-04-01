package org.jellyfin.androidtv.di

import androidx.core.content.getSystemService
import org.jellyfin.androidtv.auth.AccountManagerHelper
import org.jellyfin.androidtv.auth.ApiBinder
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.AuthenticationRepositoryImpl
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.auth.SessionRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val authModule = module {
	single { AuthenticationStore(androidContext()) }
	single { AccountManagerHelper(androidContext().getSystemService()!!) }
	single<AuthenticationRepository> {
		AuthenticationRepositoryImpl(get(), get(), get(), get(), get(userApiClient), get(), get(defaultDeviceInfo))
	}
	single<SessionRepository> {
		SessionRepositoryImpl(get(), get(), get(), get(), get(userApiClient), get(systemApiClient), get(), get(defaultDeviceInfo), get())
	}
	single { ApiBinder(get(), get()) }
}
