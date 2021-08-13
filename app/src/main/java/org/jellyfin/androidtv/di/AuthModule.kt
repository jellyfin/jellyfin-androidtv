package org.jellyfin.androidtv.di

import androidx.core.content.getSystemService
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.auth.*
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val authModule = module {
	single { AuthenticationStore(androidContext()) }
	single { AccountManagerHelper(androidContext().getSystemService()!!) }
	single<AuthenticationRepository> {
		AuthenticationRepositoryImpl(get(), get(), get(), get(), get(), get(userApiClient), get())
	}
	single<SessionRepository> {
		SessionRepositoryImpl(get(), get(), get(), get(), get(userApiClient), get(systemApiClient))
	}
	single { LegacyAccountMigration(androidContext(), get(), get(), get()) }
	single { ApiBinder(androidApplication() as JellyfinApplication, get(), get(), get()) }
}
