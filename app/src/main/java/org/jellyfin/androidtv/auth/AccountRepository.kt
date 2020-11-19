package org.jellyfin.androidtv.auth

import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import androidx.core.content.getSystemService
import timber.log.Timber
import java.util.*
import android.accounts.Account as AndroidAccount

data class Account(
//	val id: UUID,
	val server: String,
	val username: String,
	val accessToken: String?,
)

class AccountRepository(
	private val context: Context
) {
	companion object {
		const val ACCOUNT_TYPE = "org.jellyfin.v1"
	}

	private val accountManager
		get() = context.getSystemService<AccountManager>()!!

	fun createAccount(account: Account): Boolean {
		val androidAccount = AndroidAccount(account.username + "@" + account.server, ACCOUNT_TYPE)

		val data = Bundle().apply {
//			putString("id", account.id.toString())
			putString("server", account.server.toString())
			putString("username", account.username)
			putString("accessToken", account.accessToken)
		}

		accountManager.addAccountExplicitly(androidAccount, "", data)

		return true
	}

	fun getAccount(account: UUID): Account = null!!

	fun getAccounts(): Map<String, Collection<Account>> {
		val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)

		return accounts.map {account->
			Account(
				accountManager.getUserData(account, "server"),
				account.name,
				accountManager.getUserData(account, "accessToken")
			)
		}.groupBy { it.server }.also { Timber.i("Servers $it") }
	}

	fun getAccountsByServer(server: UUID): Collection<Account> = emptySet()
}
