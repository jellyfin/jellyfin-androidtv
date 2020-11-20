package org.jellyfin.androidtv.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import org.jellyfin.androidtv.auth.model.AccountManagerAccount
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Constants used in AccountManager
const val ACCOUNT_TYPE = "org.jellyfin.v1"
const val ACCOUNT_DATA_ID = "$ACCOUNT_TYPE.id"
const val ACCOUNT_DATA_SERVER = "$ACCOUNT_TYPE.server"
const val ACCOUNT_DATA_NAME = "$ACCOUNT_TYPE.name"
const val ACCOUNT_ACCESS_TOKEN_TYPE = "$ACCOUNT_TYPE.access_token.v1"

// Helper functions
private fun String.toUUID(): UUID = UUID.fromString(this)

private fun AccountManager.getAccountData(account: Account): AccountManagerAccount = AccountManagerAccount(
	id = getUserData(account, ACCOUNT_DATA_ID).toUUID(),
	server = getUserData(account, ACCOUNT_DATA_SERVER),
	name = getUserData(account, ACCOUNT_DATA_NAME),
	accessToken = peekAuthToken(account, ACCOUNT_ACCESS_TOKEN_TYPE)
)

// Extensions
suspend fun AccountManager.putJellyfinAccount(accountManagerAccount: AccountManagerAccount) {
	var androidAccount = getAccountsByType(ACCOUNT_TYPE)
		.first { getUserData(it, ACCOUNT_DATA_ID) == accountManagerAccount.id.toString() }

	// Update credentials
	if (androidAccount == null) {
		androidAccount = Account(accountManagerAccount.name, ACCOUNT_TYPE)

		addAccountExplicitly(
			androidAccount,
			"", // Leave password empty
			Bundle()
		)
	}

	// Update name
	if (androidAccount.name != accountManagerAccount.name) {
		androidAccount = suspendCoroutine { continuation ->
			renameAccount(androidAccount, accountManagerAccount.name, { continuation.resume(it.result) }, null)
		}
	}

	setUserData(androidAccount, ACCOUNT_DATA_NAME, accountManagerAccount.name)
	setUserData(androidAccount, ACCOUNT_DATA_SERVER, accountManagerAccount.server)
	setAuthToken(androidAccount, ACCOUNT_ACCESS_TOKEN_TYPE, accountManagerAccount.accessToken)
}

fun AccountManager.removeJellyfinAccount(accountManagerAccount: AccountManagerAccount) {
	val androidAccount = getAccountsByType(ACCOUNT_TYPE)
		.first { getUserData(it, ACCOUNT_DATA_ID) == accountManagerAccount.id.toString() }

	// Remove current account info
	@Suppress("DEPRECATION")
	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1)
		removeAccountExplicitly(androidAccount)
	else
		removeAccount(androidAccount, null, null)
}

fun AccountManager.getJellyfinAccounts() = getAccountsByType(ACCOUNT_TYPE).map(::getAccountData)

fun AccountManager.getJellyfinServerAccounts(server: UUID) = getAccountsByType(ACCOUNT_TYPE).filter { account ->
	getUserData(account, ACCOUNT_DATA_SERVER) == server.toString()
}.map(::getAccountData)


fun AccountManager.getJellyfinAccount(id: UUID) = getAccountsByType(ACCOUNT_TYPE).first { account ->
	getUserData(account, ACCOUNT_DATA_ID) == id.toString()
}?.let(::getAccountData)
