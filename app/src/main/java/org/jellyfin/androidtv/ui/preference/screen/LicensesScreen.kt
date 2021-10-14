package org.jellyfin.androidtv.ui.preference.screen

import androidx.core.os.bundleOf
import com.mikepenz.aboutlibraries.Libs
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.lazyOptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.link

class LicensesScreen : OptionsFragment() {
	override val screen by lazyOptionsScreen {
		when (val library = arguments?.getString(EXTRA_LIBRARY)) {
			null -> createList()
			else -> createLibrary(library)
		}
	}

	private fun OptionsScreen.createLibrary(artifactId: String) {
		val lib = Libs(requireContext()).libraries.find {
			it.libraryArtifactId == artifactId
		} ?: return createList()

		title = lib.libraryName

		category {
			@OptIn(ExperimentalStdlibApi::class)
			buildList {
				if (lib.libraryDescription.isNotBlank()) add(getString(R.string.license_description) to lib.libraryDescription)
				if (lib.libraryVersion.isNotBlank()) add(getString(R.string.license_version) to lib.libraryVersion)
				if (lib.libraryWebsite.isNotBlank()) add(getString(R.string.license_website) to lib.libraryWebsite)
				if (lib.repositoryLink.isNotBlank()) add(getString(R.string.license_repository) to lib.repositoryLink)
				if (lib.author.isNotBlank()) add(getString(R.string.license_author) to lib.author)
				lib.licenses?.forEach { license -> add(getString(R.string.license_license) to license.licenseName) }

			}.forEach { (key, value) ->
				link {
					title = value
					content = key
				}
			}
		}
	}

	private fun OptionsScreen.createList() {
		setTitle(R.string.licenses_link)

		category {
			for (library in Libs(context).libraries.sortedBy { it.libraryName.lowercase() }) {
				link {
					title = "${library.libraryName} ${library.libraryVersion}"
					content = library.licenses?.joinToString(", ") { it.licenseName }

					withFragment<LicensesScreen>(bundleOf(EXTRA_LIBRARY to library.libraryArtifactId))
				}
			}
		}
	}

	companion object {
		const val EXTRA_LIBRARY = "library"
	}
}
