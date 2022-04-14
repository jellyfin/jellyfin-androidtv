package org.jellyfin.androidtv.auth.store

import android.content.Context
import org.jellyfin.androidtv.auth.model.AuthenticationSortBy
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

class AuthenticationPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("authentication", Context.MODE_PRIVATE)
) {
	companion object {
		// Preferences
		val autoLoginUserBehavior = enumPreference("auto_login_user_behavior", UserSelectBehavior.LAST_USER)
		val autoLoginUserId = stringPreference("auto_login_user_id", "")

		val sortBy = enumPreference("sort_by", AuthenticationSortBy.LAST_USE)
		val alwaysAuthenticate = booleanPreference("always_authenticate", false)

		// Persistent state
		val lastUserId = stringPreference("last_user_id", "")
	}
}
