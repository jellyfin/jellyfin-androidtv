package org.jellyfin.androidtv.ui.settings.screen.license

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn

@Composable
fun SettingsLicenseScreen(artifactId: String) {
	val context = LocalContext.current
	val clipboard = LocalClipboard.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope

	val library = remember(context, artifactId) {
		val libs = Libs.Builder()
			.withContext(context)
			.build()

		libs.libraries.find { it.artifactId == artifactId }
	}

	if (library == null) {
		Text("Unknown library $artifactId")
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
				onClick = {
					lifecycleScope.launch {
						clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(title, value)))
						if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
							Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
						}
					}
				}
			)
		}
	}
}
