package org.jellyfin.androidtv.ui.preference.screen

import androidx.core.os.bundleOf
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen

class LicensesScreen : OptionsFragment() {
	override val screen by optionsScreen {
		when (val library = arguments?.getString(EXTRA_LIBRARY)) {
			null -> createList()
			else -> createLibrary(library)
		}
	}

	private val libs by lazy {
		Libs.Builder().withContext(requireContext()).build()
	}

	private fun OptionsScreen.createLibrary(artifactId: String) {
		val lib = libs.libraries.find {
			it.artifactId == artifactId
		} ?: return createList()

		title = lib.name

		category {
			buildList {
				add(getString(R.string.license_description) to lib.description)
				add(getString(R.string.license_version) to lib.artifactVersion)
				add(getString(R.string.license_artifact) to lib.artifactId)
				add(getString(R.string.license_website) to lib.website)
				add(getString(R.string.license_repository) to lib.scm?.url)
				lib.developers.forEach { developer -> add(getString(R.string.license_author) to developer.name) }
				lib.licenses.forEach { license -> add(getString(R.string.license_license) to license.name) }
			}.forEach { (key, value) ->
				if (value != null) {
					link {
						title = value
						content = key
					}
				}
			}
		}
	}

	private fun OptionsScreen.createList() {
		setTitle(R.string.licenses_link)

		category {
			for (library in libs.libraries.sortedBy { it.name.lowercase() }) {
				link {
					title = "${library.name} ${library.artifactVersion}"
					content = library.licenses.joinToString(", ") { it.name }

					withFragment<LicensesScreen>(bundleOf(EXTRA_LIBRARY to library.artifactId))
				}
			}
		}
	}

	companion object {
		const val EXTRA_LIBRARY = "library"
	}
}
