package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.preference.AuthenticationPreferences
import org.jellyfin.androidtv.preference.LiveTvPreferences
import org.jellyfin.androidtv.preference.PreferencesRepository
import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val preferenceModule = module {
	single { PreferencesRepository(get(userApiClient), get(), get()) }

	single { LiveTvPreferences(get(userApiClient)) }
	single { UserSettingPreferences(get(userApiClient)) }
	single { AuthenticationPreferences(androidApplication()) }
	single { UserPreferences(androidApplication()) }
	single { SystemPreferences(androidApplication()) }
}
