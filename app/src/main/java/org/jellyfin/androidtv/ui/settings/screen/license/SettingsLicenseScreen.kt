package org.jellyfin.androidtv.ui.settings.screen.license

import android.content.ClipData
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
import org.jellyfin.androidtv.ui.base.list.ListMessage
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.util.copyAction

@Composable
fun SettingsLicenseScreen(artifactId: String) {
	val context = LocalContext.current

	val library = remember(context, artifactId) {
		val libs = Libs.Builder()
			.withContext(context)
			.build()

		libs.libraries.find { it.artifactId == artifactId }
	}

	if (library == null) {
		ListMessage {
			Text("Unknown library $artifactId")
		}

		return
	}

	val metadata = buildList {
		add(stringResource(R.string.license_description) to library.description)
		add(stringResource(R.string.license_version) to library.artifactVersion)
		add(stringResource(R.string.license_artifact) to library.artifactId)
		add(stringResource(R.string.license_website) to library.website)
		add(stringResource(R.string.license_repository) to library.scm?.url)
		library.developers.forEach { developer -> add(stringResource(R.string.license_author) to developer.name) }
		library.licenses.forEach { license -> add(stringResource(R.string.license_license) to license.name) }
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.licenses_link).uppercase()) },
				headingContent = { Text(library.name) },
				captionContent = { Text(library.artifactVersion.orEmpty()) },
			)
		}

		items(metadata) { (title, value) ->
			ListButton(
				headingContent = { Text(title) },
				captionContent = { Text(value.orEmpty()) },
				onClick = copyAction(ClipData.newPlainText(title, value)),
			)
		}
	}
}
