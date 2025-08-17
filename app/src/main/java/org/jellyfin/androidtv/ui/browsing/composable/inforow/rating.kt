package org.jellyfin.androidtv.ui.browsing.composable.inforow

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Text
import java.text.NumberFormat

/**
 * A community rating item in the [BaseItemInfoRow].
 *
 * @param communityRating Between 0f and 1f.
 */
@Composable
fun InfoRowCommunityRating(communityRating: Float) {
	InfoRowItem(
		icon = ImageVector.vectorResource(R.drawable.ic_star),
		iconTint = Color(0xFFEECE55),
		contentDescription = stringResource(R.string.lbl_community_rating),
	) {
		Text(String.format("%.1f", communityRating * 10f))
	}
}

private const val CRITIC_RATING_FRESH = 0.6f

/**
 * A critic rating item in the [BaseItemInfoRow].
 *
 * @param criticRating Between 0f and 1f.
 */
@Composable
fun InfoRowCriticRating(criticRating: Float) {
	InfoRowItem(
		icon = when {
			criticRating >= CRITIC_RATING_FRESH -> ImageVector.vectorResource(R.drawable.ic_rt_fresh)
			else -> ImageVector.vectorResource(R.drawable.ic_rt_rotten)
		},
		iconTint = Color.Unspecified,
		contentDescription = stringResource(R.string.lbl_critic_rating),
	) {
		Text(NumberFormat.getPercentInstance().format(criticRating))
	}
}

/**
 * A parental rating item in the [BaseItemInfoRow].
 */
@Composable
fun InfoRowParentalRating(parentalRating: String) {
	InfoRowItem(
		contentDescription = stringResource(R.string.lbl_rating),
		colors = InfoRowColors.Default,
	) {
		Text(parentalRating)
	}
}
