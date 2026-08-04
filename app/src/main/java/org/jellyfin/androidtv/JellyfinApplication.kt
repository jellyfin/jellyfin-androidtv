package org.jellyfin.androidtv

import android.app.Application
import android.content.Context
import org.jellyfin.androidtv.telemetry.TelemetryService

class JellyfinApplication : Application() {
	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		TelemetryService.init(this)
	}
}
