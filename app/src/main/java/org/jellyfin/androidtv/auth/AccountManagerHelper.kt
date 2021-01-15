package org.jellyfin.androidtv.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import org.jellyfin.androidtv.util.toUUID
import org.jellyfin.androidtv.util.toUUIDOrNull
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AccountManagerHelper(
	private val accountManager: AccountManager
) {
	companion object {
		const val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID
		const val ACCOUNT_DATA_ID = "$ACCOUNT_TYPE.id"
		const val ACCOUNT_DATA_SERVER = "$ACCOUNT_TYPE.server"
		const val ACCOUNT_DATA_NAME = "$ACCOUNT_TYPE.name"
		const val ACCOUNT_ACCESS_TOKEN_TYPE = "$ACCOUNT_TYPE.access_token"
	}

	private fun getAccountData(account: Account): AccountManagerAccount = AccountManagerAccount(
		id = accountManager.getUserData(account, ACCOUNT_DATA_ID).toUUID(),
		server = accountManager.getUserData(account, ACCOUNT_DATA_SERVER).toUUID(),
		name = accountManager.getUserData(account, ACCOUNT_DATA_NAME),
		accessToken = accountManager.peekAuthToken(account, ACCOUNT_ACCESS_TOKEN_TYPE)
	)

	suspend fun putAccount(accountManagerAccount: AccountManagerAccount) {
		var androidAccount = accountManager.getAccountsByType(ACCOUNT_TYPE)
			.firstOrNull { accountManager.getUserData(it, ACCOUNT_DATA_ID)?.toUUIDOrNull() == accountManagerAccount.id }

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

	fun removeAccount(accountManagerAccount: AccountManagerAccount) {
		val androidAccount = accountManager.getAccountsByType(ACCOUNT_TYPE)
			.first { accountManager.getUserData(it, ACCOUNT_DATA_ID)?.toUUIDOrNull() == accountManagerAccount.id }

		// Remove current account info
		@Suppress("DEPRECATION")
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
			accountManager.removeAccountExplicitly(androidAccount)
		else
			accountManager.removeAccount(androidAccount, null, null)
	}

	fun getAccounts() = accountManager.getAccountsByType(ACCOUNT_TYPE).map(::getAccountData)

	fun getAccountsByServer(server: UUID) = accountManager.getAccountsByType(ACCOUNT_TYPE).filter { account ->
		accountManager.getUserData(account, ACCOUNT_DATA_SERVER)?.toUUIDOrNull() == server
	}.map(::getAccountData)

	fun getAccount(id: UUID) = accountManager.getAccountsByType(ACCOUNT_TYPE).firstOrNull { account ->
		accountManager.getUserData(account, ACCOUNT_DATA_ID)?.toUUIDOrNull() == id
	}?.let(::getAccountData)
}
