package org.jellyfin.androidtv.ui.browsing.composable.inforow

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import java.text.NumberFormat

/**
 * A community rating item in the [BaseItemInfoRow].
 *
 * @param communityRating Between 0f and 1f.
 */
@Composable
fun InfoRowCommunityRating(communityRating: Float) {
	InfoRowItem(
		icon = painterResource(R.drawable.ic_star),
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
			criticRating >= CRITIC_RATING_FRESH -> painterResource(R.drawable.ic_rt_fresh)
			else -> painterResource(R.drawable.ic_rt_rotten)
		},
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
