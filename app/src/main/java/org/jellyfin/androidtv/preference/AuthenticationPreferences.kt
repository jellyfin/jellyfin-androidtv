package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior

class AuthenticationPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("authentication", Context.MODE_PRIVATE)
) {
	companion object {
		val autoLoginUserBehavior = Preference.enum<UserSelectBehavior>("auto_login_user_behavior", UserSelectBehavior.LAST_USER)
		val autoLoginUserId = Preference.string("auto_login_user_id", "")

		val serviceUserBehavior = Preference.enum<UserSelectBehavior>("service_user_behavior", UserSelectBehavior.LAST_USER)
		val serviceUserId = Preference.string("service_user_id", "")
	}
}
