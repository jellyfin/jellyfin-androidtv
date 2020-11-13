package org.jellyfin.androidtv.di

import android.app.Application
import org.jellyfin.androidtv.ui.shared.AppThemeCallbacks
import org.jellyfin.androidtv.ui.shared.AuthenticatedUserCallbacks
import org.koin.dsl.bind
import org.koin.dsl.module

val activityLifecycleCallbacksModule = module {
	single { AuthenticatedUserCallbacks() } bind Application.ActivityLifecycleCallbacks::class
	single { AppThemeCallbacks(get()) } bind Application.ActivityLifecycleCallbacks::class
}
