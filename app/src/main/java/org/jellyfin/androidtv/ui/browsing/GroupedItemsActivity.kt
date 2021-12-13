package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace

class GroupedItemsActivity : FragmentActivity() {
	private val groupingType get() = intent.extras?.getString(EXTRA_GROUPING_TYPE)?.let { GroupingType.valueOf(it) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportFragmentManager.commit {
			when (groupingType) {
				GroupingType.GENRE -> replace<ByGenreFragment>(android.R.id.content)
				GroupingType.LETTER -> replace<ByLetterFragment>(android.R.id.content)
			}
		}
	}

	enum class GroupingType {
		GENRE,
		LETTER,
	}

	companion object {
		const val EXTRA_GROUPING_TYPE = "type_grouping"
		const val EXTRA_FOLDER = "folder"
		const val EXTRA_INCLUDE_TYPE = "type_include"
	}
}
