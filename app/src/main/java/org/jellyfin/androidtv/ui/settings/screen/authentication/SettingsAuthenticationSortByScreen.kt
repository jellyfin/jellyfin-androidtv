package org.jellyfin.androidtv.ui.settings.screen.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.AuthenticationSortBy
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.koin.compose.koinInject

@Composable
fun SettingsAuthenticationSortByScreen() {
	val router = LocalRouter.current
	val authenticationPreferences = koinInject<AuthenticationPreferences>()
	var sortBy by rememberPreference(authenticationPreferences, AuthenticationPreferences.sortBy)

	LazyColumn(
		modifier = Modifier
			.padding(6.dp),
		verticalArrangement = Arrangement.spacedBy(4.dp),
	) {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
				headingContent = { Text(stringResource(R.string.sort_accounts_by)) },
			)
		}

		items(AuthenticationSortBy.entries) { entry ->
			ListButton(
				headingContent = { Text(stringResource(entry.nameRes)) },
				trailingContent = { RadioButton(checked = sortBy == entry) },
				onClick = {
					sortBy = entry
					router.back()
				}
			)
		}
	}
}
