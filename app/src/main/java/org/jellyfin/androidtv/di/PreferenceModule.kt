package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.preference.*
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.binds
import org.koin.dsl.module

val preferenceModule = module {
	single { PreferencesRepository(get(userApiClient), get(), get()) }

	single { LiveTvPreferences(get(userApiClient)) }
	single { UserSettingPreferences(get(userApiClient)) }
	single { AuthenticationPreferences(androidApplication()) }
	single { UserPreferences(androidApplication()) } binds arrayOf(
		UserPreferences::class,
		IUserPreferences::class
	)
	single { SystemPreferences(androidApplication()) }
}
