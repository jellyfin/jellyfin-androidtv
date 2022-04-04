package org.jellyfin.androidtv.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.work.WorkManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Provides DI for Android system components
 */
val androidModule = module {
	factory { androidApplication().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
	factory { WorkManager.getInstance(androidContext()) }
}
