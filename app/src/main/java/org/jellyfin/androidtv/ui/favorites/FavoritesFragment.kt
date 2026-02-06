package org.jellyfin.androidtv.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.content
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.Extras
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.browsing.FavoritesBrowseFragment
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbar
import org.jellyfin.androidtv.ui.shared.toolbar.MainToolbarActiveButton
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType

class FavoritesFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		val gridFocusRequester = remember { FocusRequester() }
		LaunchedEffect(gridFocusRequester) { gridFocusRequester.requestFocus() }

		JellyfinTheme {
			val title = stringResource(R.string.lbl_favorites)
			val fragmentArgs = remember(title) {
				val folderId = UUID.nameUUIDFromBytes("jellyfin-androidtv-favorites".toByteArray())
				val preferencesId = UUID.nameUUIDFromBytes("jellyfin-androidtv-favorites-display".toByteArray())
				val folder = BaseItemDto(
					id = folderId,
					name = title,
					type = BaseItemKind.FOLDER,
					collectionType = CollectionType.UNKNOWN,
					displayPreferencesId = preferencesId.toString(),
				)
				bundleOf(
					Extras.Folder to Json.encodeToString(BaseItemDto.serializer(), folder),
				)
			}

			Column {
				MainToolbar(MainToolbarActiveButton.Favorites)

				AndroidFragment<FavoritesBrowseFragment>(
					modifier = Modifier
						.focusGroup()
						.focusRequester(gridFocusRequester)
						.fillMaxSize(),
					arguments = fragmentArgs,
				)
			}
		}
	}
}
