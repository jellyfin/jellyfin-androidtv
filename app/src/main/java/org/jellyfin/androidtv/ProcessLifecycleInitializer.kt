package org.jellyfin.androidtv

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.di.KoinInitializer
import timber.log.Timber

@Suppress("unused")
class ProcessLifecycleInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		val koin = AppInitializer.getInstance(context)
			.initializeComponent(KoinInitializer::class.java)
			.koin

		// Register application lifecycle events
		ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
			/**
			 * Called by the Process Lifecycle when the app is created. It is called after [onCreate].
			 */
			override fun onCreate(owner: LifecycleOwner) {
				// Register activity lifecycle callbacks
				val callbacks = koin.getAll<Application.ActivityLifecycleCallbacks>()
				Timber.i("Registering ${callbacks.size} ActivityLifecycleCallbacks")
				val app = context.applicationContext as Application
				callbacks.forEach { callback -> app.registerActivityLifecycleCallbacks(callback) }
			}

			/**
			 * Called by the Process Lifecycle when the app is activated in the foreground (activity opened).
			 */
			override fun onStart(owner: LifecycleOwner) {
				Timber.i("Process lifecycle started")

				owner.lifecycleScope.launch {
					koin.get<SessionRepository>().restoreSession()
				}
			}
		})
	}

	override fun dependencies() = listOf(KoinInitializer::class.java)
}
