package org.jellyfin.androidtv

import android.content.Context
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraHttpSender
import org.acra.annotation.AcraLimiter
import org.acra.sender.HttpSender
import org.jellyfin.androidtv.di.appModule
import org.jellyfin.androidtv.di.playbackModule
import org.jellyfin.androidtv.di.preferenceModule
import org.jellyfin.androidtv.ui.shared.AppThemeCallbacks
import org.jellyfin.androidtv.ui.shared.AuthenticatedUserCallbacks
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import timber.log.Timber.DebugTree

@AcraCore(
	buildConfigClass = BuildConfig::class
)
@AcraHttpSender(
	uri = "https://collector.tracepot.com/a2eda9d9",
	httpMethod = HttpSender.Method.POST
)
@AcraDialog(
	resTitle = R.string.acra_dialog_title,
	resText = R.string.acra_dialog_text,
	resTheme = R.style.Theme_Jellyfin
)
@AcraLimiter
class JellyfinApplication : TvApp() {
	override fun onCreate() {
		super.onCreate()

		// Dependency Injection
		startKoin {
			// Temporary disabled until Koin is updated to 2.2 >=
			// androidLogger()
			androidContext(this@JellyfinApplication)

			modules(
				appModule,
				playbackModule,
				preferenceModule
			)
		}

		// Register lifecycle callbacks
		registerActivityLifecycleCallbacks(AuthenticatedUserCallbacks())
		registerActivityLifecycleCallbacks(AppThemeCallbacks())

		// Initialize the logging library
		Timber.plant(DebugTree())
		Timber.i("Application object created")
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)

		ACRA.init(this)
	}
}
