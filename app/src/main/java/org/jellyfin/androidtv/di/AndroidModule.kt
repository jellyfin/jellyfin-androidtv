package org.jellyfin.androidtv.di

import android.accounts.AccountManager
import androidx.core.content.getSystemService
import androidx.work.WorkManager
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/**
 * Provides DI for Android system components
 */
val androidModule = module {
	factory { androidApplication().getSystemService<AccountManager>()!! }
	factory { WorkManager.getInstance(get()) }
}
