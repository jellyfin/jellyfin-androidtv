package org.jellyfin.androidtv

import android.content.Context
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraHttpSender
import org.acra.annotation.AcraLimiter
import org.acra.sender.HttpSender
import org.jellyfin.androidtv.di.activityLifecycleCallbacksModule
import org.jellyfin.androidtv.di.appModule
import org.jellyfin.androidtv.di.playbackModule
import org.jellyfin.androidtv.di.preferenceModule
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
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
@Suppress("unused")
class JellyfinApplication : TvApp() {
	override fun onCreate() {
		super.onCreate()

		// Initialize the logging library
		Timber.plant(DebugTree())
		Timber.i("Application object created")

		// Dependency Injection
		startKoin {
			androidLogger()
			androidContext(this@JellyfinApplication)

			modules(
				appModule,
				activityLifecycleCallbacksModule,
				playbackModule,
				preferenceModule
			)
		}

		// Register lifecycle callbacks
		getKoin().getAll<ActivityLifecycleCallbacks>().forEach(::registerActivityLifecycleCallbacks)

		// Enable improved logging for leaking resources
		// https://wh0.github.io/2020/08/12/closeguard.html
		if (BuildConfig.DEBUG) {
			try {
				Class.forName("dalvik.system.CloseGuard")
					.getMethod("setEnabled", Boolean::class.javaPrimitiveType)
					.invoke(null, true)
			} catch (e: ReflectiveOperationException) {
				@Suppress("TooGenericExceptionThrown")
				throw RuntimeException(e)
			}
		}
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)

		ACRA.init(this)
	}
}
