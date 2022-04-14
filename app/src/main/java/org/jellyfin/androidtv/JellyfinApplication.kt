package org.jellyfin.androidtv

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.config.limiter
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.data.eventhandling.SocketHandler
import org.jellyfin.androidtv.integration.LeanbackChannelWorker
import org.jellyfin.androidtv.util.AutoBitrate
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@Suppress("unused")
class JellyfinApplication : Application() {
	override fun onCreate() {
		super.onCreate()

		// Register application lifecycle events
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
			/**
			 * Called by the Process Lifecycle when the app is created. It is called after [onCreate].
			 */
			override fun onCreate(owner: LifecycleOwner) {
				// Register activity lifecycle callbacks
				getKoin().getAll<ActivityLifecycleCallbacks>().forEach(::registerActivityLifecycleCallbacks)
			}

			/**
			 * Called by the Process Lifecycle when the app is activated in the foreground (activity opened).
			 */
			override fun onStart(owner: LifecycleOwner) {
				Timber.i("Process lifecycle started")

				owner.lifecycleScope.launch {
					get<SessionRepository>().restoreSession()
				}
			}
		})
	}

	/**
	 * Called from the StartupActivity when the user session is started.
	 */
	suspend fun onSessionStart() = withContext(Dispatchers.IO) {
		val workManager by inject<WorkManager>()
		val autoBitrate by inject<AutoBitrate>()
		val socketListener by inject<SocketHandler>()

		// Update background worker
		launch {
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
		}

		// Update WebSockets
		launch { socketListener.updateSession() }

		// Detect auto bitrate
		// running in a different scope to prevent slow startups
		ProcessLifecycleOwner.get().lifecycleScope.launch { autoBitrate.detect() }
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
