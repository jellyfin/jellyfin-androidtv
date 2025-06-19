package org.jellyfin.androidtv.di

import android.app.UiModeManager
import android.media.AudioManager
import androidx.core.content.getSystemService
import androidx.work.WorkManager
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/**
 * Provides DI for Android system components
 */
val androidModule = module {
	factory { 
		androidApplication().getSystemService<UiModeManager>() 
			?: throw IllegalStateException("UiModeManager service not available")
	}
	factory { 
		androidApplication().getSystemService<AudioManager>() 
			?: throw IllegalStateException("AudioManager service not available")
	}
	factory { WorkManager.getInstance(get()) }
}
