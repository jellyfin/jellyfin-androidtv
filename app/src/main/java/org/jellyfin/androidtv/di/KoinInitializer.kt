package org.jellyfin.androidtv.di

import android.content.Context
import androidx.startup.Initializer
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

@Suppress("unused")
class KoinInitializer : Initializer<KoinApplication> {
	override fun create(context: Context): KoinApplication = startKoin {
		androidContext(context)

		modules(
			appModule,
			authModule,
			activityLifecycleCallbacksModule,
			playbackModule,
			preferenceModule,
			utilsModule
		)
	}

	override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
