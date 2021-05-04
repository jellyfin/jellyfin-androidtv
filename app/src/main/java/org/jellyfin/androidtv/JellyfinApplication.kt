package org.jellyfin.androidtv

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.runBlocking
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraHttpSender
import org.acra.annotation.AcraLimiter
import org.acra.sender.HttpSender
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.di.*
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinExperimentalAPI
import org.koin.core.context.startKoin
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.TimeUnit

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
	@OptIn(KoinExperimentalAPI::class)
	override fun onCreate() {
		super.onCreate()

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

		// Initialize the logging library
		Timber.plant(DebugTree())
		Timber.i("Application object created")

		// Dependency Injection
		startKoin {
			androidLogger()
			androidContext(this@JellyfinApplication)

			modules(
				appModule,
				authModule,
				activityLifecycleCallbacksModule,
				playbackModule,
				preferenceModule,
				utilsModule
			)
		}

		// Setup background workers
		runBlocking {
			val workManager by inject<WorkManager>()

			// Cancel all workers
			workManager.cancelAllWork().await()

			// Recreate periodic workers
			workManager.enqueueUniquePeriodicWork(
				LeanbackChannelWorker.PERIODIC_UPDATE_REQUEST_NAME,
				ExistingPeriodicWorkPolicy.REPLACE,
				PeriodicWorkRequestBuilder<LeanbackChannelWorker>(1, TimeUnit.HOURS).build()
			).await()
		}

		// Register lifecycle callbacks
		getKoin().getAll<ActivityLifecycleCallbacks>().forEach(::registerActivityLifecycleCallbacks)

		// Restore session
		get<SessionRepository>().apply {
			restoreDefaultSession()
			restoreDefaultSystemSession()
		}
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)

		ACRA.init(this)
	}
}
