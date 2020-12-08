package org.jellyfin.androidtv.di

import androidx.core.content.getSystemService
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.auth.AccountManagerHelper
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.jellyfin.androidtv.auth.LegacyAccountMigration
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val authModule = module {
	single { AuthenticationStore(androidContext()) }
	single { AccountManagerHelper(androidContext().getSystemService()!!) }
	single { AuthenticationRepository(androidApplication() as JellyfinApplication, get(), get(), get(), get(), get()) }
	single { LegacyAccountMigration(androidContext(), get(), get()) }
}
