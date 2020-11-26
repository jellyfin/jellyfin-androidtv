package org.jellyfin.androidtv.di

import androidx.core.content.getSystemService
import org.jellyfin.androidtv.auth.AccountManagerHelper
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.auth.AuthenticationStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val authModule = module {
	single { AuthenticationStore(androidContext()) }
	single { AccountManagerHelper(androidContext().getSystemService()!!) }
	single { AuthenticationRepository(get(), get(), get(), get()) }
}
