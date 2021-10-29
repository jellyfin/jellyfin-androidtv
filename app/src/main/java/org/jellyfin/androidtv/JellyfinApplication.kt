package org.jellyfin.androidtv

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.config.limiter
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.util.AutoBitrate
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import org.koin.core.KoinExperimentalAPI
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.TimeUnit

@Suppress("unused")
class JellyfinApplication : TvApp(), LifecycleObserver {
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

		// Register lifecycle callbacks
		getKoin().getAll<ActivityLifecycleCallbacks>().forEach(::registerActivityLifecycleCallbacks)

		ProcessLifecycleOwner.get().lifecycle.addObserver(this)

		// Restore system session
		get<SessionRepository>().restoreDefaultSystemSession()
	}

	/**
	 * Called by the Process Lifecycle when the app is activated in the foreground (activity opened).
	 */
	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	fun onActivate() {
		Timber.i("Process Lifecycle started")

		get<SessionRepository>().restoreDefaultSession()
	}

	/**
	 * Called from the StartupActivity when the user session is started.
	 */
	@DelicateCoroutinesApi
	suspend fun onSessionStart() {
		val workManager by inject<WorkManager>()
		val autoBitrate by inject<AutoBitrate>()

		// Cancel all current workers
		workManager.cancelAllWork().await()

		// Recreate periodic workers
		workManager.enqueueUniquePeriodicWork(
			LeanbackChannelWorker.PERIODIC_UPDATE_REQUEST_NAME,
			ExistingPeriodicWorkPolicy.REPLACE,
			PeriodicWorkRequestBuilder<LeanbackChannelWorker>(1, TimeUnit.HOURS)
				.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
				.build()
		).await()

		// Detect auto bitrate
		GlobalScope.launch(Dispatchers.IO) { autoBitrate.detect() }
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)

		initAcra {
			buildConfigClass = BuildConfig::class.java
			reportFormat = StringFormat.JSON

			httpSender {
				uri = "https://collector.tracepot.com/a2eda9d9"
				httpMethod = HttpSender.Method.POST
			}

			dialog {
				withResTitle(R.string.acra_dialog_title)
				withResText(R.string.acra_dialog_text)
				withResTheme(R.style.Theme_Jellyfin)
			}

			limiter {}
		}
	}
}
