package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.androidtv.auth.AuthenticationSortBy
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior

class AuthenticationPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("authentication", Context.MODE_PRIVATE)
) {
	companion object {
		val autoLoginUserBehavior = Preference.enum("auto_login_user_behavior", UserSelectBehavior.LAST_USER)
		val autoLoginUserId = Preference.string("auto_login_user_id", "")

		val systemUserBehavior = Preference.enum("system_user_behavior", UserSelectBehavior.LAST_USER)
		val systemUserId = Preference.string("system_user_id", "")

		val sortBy = Preference.enum("sort_by", AuthenticationSortBy.LAST_USE)
		val alwaysAuthenticate = Preference.boolean("always_authenticate", false)

		/**
		 * Do not set directly, use [SessionRepository] instead.
		 */
		val lastUserId = Preference.string("last_user_id", "")
	}
}
