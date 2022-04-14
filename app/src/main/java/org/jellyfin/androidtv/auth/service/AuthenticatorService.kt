package org.jellyfin.androidtv.auth.service

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.os.bundleOf
import org.jellyfin.androidtv.ui.preference.PreferencesActivity

class AuthenticatorService : Service() {
	private val authenticator by lazy {
		Authenticator(this)
	}

	override fun onBind(intent: Intent): IBinder? = authenticator.iBinder

	private inner class Authenticator(
		private val service: AuthenticatorService
	) : AbstractAccountAuthenticator(service) {
		private val unsupportedOperationBundle = bundleOf(
			AccountManager.KEY_ERROR_CODE to AccountManager.ERROR_CODE_REMOTE_EXCEPTION,
			AccountManager.KEY_ERROR_MESSAGE to "Unsupported operation"
		)

		override fun addAccount(
			response: AccountAuthenticatorResponse,
			accountType: String,
			authTokenType: String?,
			requiredFeatures: Array<String>?,
			options: Bundle,
		): Bundle = unsupportedOperationBundle

		override fun confirmCredentials(
			response: AccountAuthenticatorResponse,
			account: Account,
			options: Bundle?,
		): Bundle = unsupportedOperationBundle

		override fun editProperties(
			response: AccountAuthenticatorResponse,
			accountType: String,
		): Bundle = bundleOf(
			AccountManager.KEY_INTENT to Intent(service, PreferencesActivity::class.java)
		)

		override fun getAccountRemovalAllowed(
			response: AccountAuthenticatorResponse,
			account: Account,
		): Bundle = bundleOf(
			AccountManager.KEY_BOOLEAN_RESULT to true
		)

		override fun getAuthToken(
			response: AccountAuthenticatorResponse,
			account: Account,
			authTokenType: String,
			options: Bundle,
		): Bundle = unsupportedOperationBundle

		override fun getAuthTokenLabel(authTokenType: String): String? = null

		override fun hasFeatures(
			response: AccountAuthenticatorResponse,
			account: Account,
			features: Array<String>,
		): Bundle = unsupportedOperationBundle

		override fun updateCredentials(
			response: AccountAuthenticatorResponse,
			account: Account,
			authTokenType: String?,
			options: Bundle?,
		): Bundle = unsupportedOperationBundle
	}
}
