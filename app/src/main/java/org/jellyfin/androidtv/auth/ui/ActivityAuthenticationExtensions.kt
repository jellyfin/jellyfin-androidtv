package org.jellyfin.androidtv.auth.ui

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Extension function to check authentication. Should be called in [FragmentActivity.onCreate] and
 * [FragmentActivity.onResume]. It validates the current session and opens the authentication screen
 * when no session is found.
 *
 * @return whether to proceed creating the activity or not. When `false` is returned the
 * [FragmentActivity.finish] function is automatically called.
 */
fun FragmentActivity.validateAuthentication(): Boolean {
	val sessionRepository by inject<SessionRepository>()
	val userRepository by inject<UserRepository>()

	if (sessionRepository.currentSession.value == null || userRepository.currentUser.value == null) {
		Timber.w("Activity ${this::class.qualifiedName} started without a session, bouncing to StartupActivity")
		startActivity(Intent(this, StartupActivity::class.java))
		finish()
		return false
	}

	return true
}
