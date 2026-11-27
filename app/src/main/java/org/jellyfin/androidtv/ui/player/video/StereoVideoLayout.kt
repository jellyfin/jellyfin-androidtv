package org.jellyfin.androidtv.ui.player.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jellyfin.sdk.model.api.Video3dFormat

internal val Video3dFormat?.hasStereoSplit: Boolean
	get() = this == Video3dFormat.HALF_SIDE_BY_SIDE ||
		this == Video3dFormat.FULL_SIDE_BY_SIDE ||
		this == Video3dFormat.HALF_TOP_AND_BOTTOM ||
		this == Video3dFormat.FULL_TOP_AND_BOTTOM

@Composable
internal fun StereoVideoLayout(
	video3dFormat: Video3dFormat?,
	modifier: Modifier = Modifier,
	content: @Composable BoxScope.() -> Unit,
) {
	when (video3dFormat) {
		Video3dFormat.HALF_SIDE_BY_SIDE,
		Video3dFormat.FULL_SIDE_BY_SIDE -> Row(modifier) {
			repeat(2) {
				Box(
					Modifier
						.weight(1f)
						.fillMaxHeight(),
					content = content,
				)
			}
		}

		Video3dFormat.HALF_TOP_AND_BOTTOM,
		Video3dFormat.FULL_TOP_AND_BOTTOM -> Column(modifier) {
			repeat(2) {
				Box(
					Modifier
						.weight(1f)
						.fillMaxWidth(),
					content = content,
				)
			}
		}

		else -> Box(modifier, content = content)
	}
}
