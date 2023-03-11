package org.jellyfin.androidtv.auth.store

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Build
import android.os.Bundle
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.sdk.model.serializer.toUUID
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AccountManagerStore(
	private val accountManager: AccountManager
) {
	companion object {
		const val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID
		const val ACCOUNT_DATA_ID = "$ACCOUNT_TYPE.id"
		const val ACCOUNT_DATA_SERVER = "$ACCOUNT_TYPE.server"
		const val ACCOUNT_DATA_NAME = "$ACCOUNT_TYPE.name"
		const val ACCOUNT_ACCESS_TOKEN_TYPE = "$ACCOUNT_TYPE.access_token"
	}

	private fun Array<Account>.filterServerAccount(server: UUID, account: UUID? = null) = filter {
		val validServerId = accountManager.getUserData(it, ACCOUNT_DATA_SERVER)?.toUUIDOrNull() == server
		val validUserId = account == null || accountManager.getUserData(it, ACCOUNT_DATA_ID)?.toUUIDOrNull() == account

		validServerId && validUserId
	}

	private fun getAccountData(account: Account): AccountManagerAccount = AccountManagerAccount(
		id = accountManager.getUserData(account, ACCOUNT_DATA_ID).toUUID(),
		server = accountManager.getUserData(account, ACCOUNT_DATA_SERVER).toUUID(),
		name = accountManager.getUserData(account, ACCOUNT_DATA_NAME),
		accessToken = accountManager.peekAuthToken(account, ACCOUNT_ACCESS_TOKEN_TYPE)
	)

	suspend fun putAccount(accountManagerAccount: AccountManagerAccount) {
		var androidAccount = accountManager.getAccountsByType(ACCOUNT_TYPE)
			.filterServerAccount(accountManagerAccount.server, accountManagerAccount.id)
			.firstOrNull()

		// Update credentials
		if (androidAccount == null) {
			androidAccount = Account(accountManagerAccount.name, ACCOUNT_TYPE)

			accountManager.addAccountExplicitly(
				androidAccount,
				"", // Leave password empty
				Bundle()
			)
			accountManager.setUserData(androidAccount, ACCOUNT_DATA_ID, accountManagerAccount.id.toString())
		}

		// Update name
		if (androidAccount.name != accountManagerAccount.name) {
			androidAccount = suspendCoroutine { continuation ->
				accountManager.renameAccount(androidAccount, accountManagerAccount.name, { continuation.resume(it.result) }, null)
			}
		}

		accountManager.setUserData(androidAccount, ACCOUNT_DATA_NAME, accountManagerAccount.name)
		accountManager.setUserData(androidAccount, ACCOUNT_DATA_SERVER, accountManagerAccount.server.toString())
		accountManager.setAuthToken(androidAccount, ACCOUNT_ACCESS_TOKEN_TYPE, accountManagerAccount.accessToken)
	}

	fun removeAccount(accountManagerAccount: AccountManagerAccount): Boolean {
		val androidAccount = accountManager.getAccountsByType(ACCOUNT_TYPE)
			.filterServerAccount(accountManagerAccount.server, accountManagerAccount.id)
			.firstOrNull()
			?: return false

		// Remove current account info
		@Suppress("DEPRECATION")
		return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
			accountManager.removeAccount(androidAccount, null, null)
			true
		} else accountManager.removeAccountExplicitly(androidAccount)
	}

	fun getAccounts() = accountManager.getAccountsByType(ACCOUNT_TYPE).map(::getAccountData)

	fun getAccountsByServer(server: UUID) = accountManager.getAccountsByType(ACCOUNT_TYPE)
		.filterServerAccount(server)
		.map(::getAccountData)

	fun getAccount(server: UUID, account: UUID) = accountManager.getAccountsByType(ACCOUNT_TYPE)
		.filterServerAccount(server, account)
		.firstOrNull()
		?.let(::getAccountData)
}
