package org.jellyfin.androidtv.ui.settings.screen.license

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn

@Composable
fun SettingsLicensesScreen() {
	val context = LocalContext.current
	val router = LocalRouter.current

	val libraries = remember(context) {
		val libs = Libs.Builder()
			.withContext(context)
			.build()

		libs.libraries.sortedBy { it.name.lowercase() }
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.settings).uppercase()) },
				headingContent = { Text(stringResource(R.string.licenses_link)) },
			)
		}

		items(libraries) { library ->
			ListButton(
				headingContent = { Text("${library.name} ${library.artifactVersion}") },
				captionContent = { Text(library.licenses.joinToString(", ") { license -> license.name }) },
				onClick = { router.push(Routes.LICENSE, mapOf("artifactId" to library.artifactId)) }
			)
		}
	}
}
