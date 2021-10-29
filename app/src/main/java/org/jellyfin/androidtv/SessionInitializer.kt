package org.jellyfin.androidtv

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.di.KoinInitializer

@Suppress("unused")
class SessionInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		val koin = AppInitializer.getInstance(context)
			.initializeComponent(KoinInitializer::class.java)
			.koin

		// Restore system session
		koin.get<SessionRepository>().restoreDefaultSystemSession()
	}

	override fun dependencies() = listOf(KoinInitializer::class.java)
}
