package org.jellyfin.androidtv.ui.composable.item

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.design.Tokens
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.compose.koinInject

/**
 * A horizontal list item composable that displays a movie with poster on the left
 * and title/metadata on the right. This is designed for use in a horizontal scrollable list.
 *
 * @param item The BaseRowItem to display
 * @param focused Whether the item is currently focused
 * @param posterHeight The height of the poster image
 * @param modifier Modifier for the composable
 */
@Composable
@Stable
fun MovieListItem(
    item: BaseRowItem?,
    focused: Boolean,
    posterHeight: Dp = 120.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val localDensity = LocalDensity.current

    if (item == null) return

    val title = remember(item, context) { item.getCardName(context) }
    val subtitle = remember(item, context) { item.getSubText(context) }
    val summary = remember(item, context) { item.getSummary(context) }
	val image = remember(item) { item.getImage(ImageType.POSTER) }

    // Calculate poster width based on typical movie poster aspect ratio (2:3)
    val posterWidth = posterHeight * 0.67f

    val backgroundColor = if (focused) {
        JellyfinTheme.colorScheme.surface.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    val focusModifier = if (focused) Modifier.basicMarquee(
        iterations = Int.MAX_VALUE,
        initialDelayMillis = 0,
    ) else Modifier

    Row(
        modifier = modifier
            .height(posterHeight + 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // Poster on the left
        MovieListItemPoster(
            image = image,
            width = posterWidth,
            height = posterHeight,
        )

        Spacer(modifier = Modifier.width(Tokens.Space.spaceMd))

        // Title and metadata on the right
        Column(
            modifier = Modifier
                .fillMaxHeight()
				.fillMaxWidth(), // geändert
            verticalArrangement = Arrangement.Center,
        ) {
            // Title
            title?.let { text ->
                Text(
                    text = text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    color = if (focused) Tokens.Color.colorGrey100 else Tokens.Color.colorGrey200,
                    modifier = Modifier.then(focusModifier),
                )
            }

            Spacer(modifier = Modifier.height(Tokens.Space.spaceXs))

            // Subtitle (year, rating, etc.)
            subtitle?.let { text ->
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = Tokens.Color.colorGrey400,
                )
            }

            // Summary/Overview (if space allows)
            summary?.let { text ->
                Spacer(modifier = Modifier.height(Tokens.Space.spaceXs))
                Text(
                    text = text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = Tokens.Color.colorGrey500,
                )
            }
        }

        // Play indicator overlay for watched/progress
        item.baseItem?.let { baseItem ->
            Box(
                modifier = Modifier
                    .padding(start = Tokens.Space.spaceXs)
            ) {
                ItemCardBaseItemOverlay(baseItem)
            }
        }
    }
}

/**
 * The poster image component for the list item
 */
@Composable
private fun MovieListItemPoster(
    image: org.jellyfin.androidtv.util.apiclient.JellyfinImage?,
    width: Dp,
    height: Dp,
) {
    val api = koinInject<ApiClient>()
    val localDensity = LocalDensity.current

    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(6.dp))
            .background(Tokens.Color.colorGrey800),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            AsyncImage(
                url = image.getUrl(
                    api,
                    maxWidth = with(localDensity) { width.roundToPx() },
                    maxHeight = with(localDensity) { height.roundToPx() },
                ),
                blurHash = image.blurHash,
                aspectRatio = width / height,
                scaleType = ImageView.ScaleType.CENTER_CROP,
                modifier = Modifier.size(width, height)
            )
        }
    }
}

/**
 * A compact version of the list item for denser layouts
 */
@Composable
@Stable
fun MovieListItemCompact(
    item: BaseRowItem?,
    focused: Boolean,
    posterHeight: Dp = 80.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    if (item == null) return

    val title = remember(item, context) { item.getCardName(context) }
    val subtitle = remember(item, context) { item.getSubText(context) }
	val image = remember(item) { item.getImage(ImageType.POSTER) }

    val posterWidth = posterHeight * 0.67f

    val backgroundColor = if (focused) {
        JellyfinTheme.colorScheme.surface.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    val focusModifier = if (focused) Modifier.basicMarquee(
        iterations = Int.MAX_VALUE,
        initialDelayMillis = 0,
    ) else Modifier

    Row(
        modifier = modifier
            .height(posterHeight + 8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Poster
        MovieListItemPoster(
            image = image,
            width = posterWidth,
            height = posterHeight,
        )

        Spacer(modifier = Modifier.width(Tokens.Space.spaceSm))

        // Title only
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(150.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            title?.let { text ->
                Text(
                    text = text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = if (focused) Tokens.Color.colorGrey100 else Tokens.Color.colorGrey200,
                    modifier = Modifier.then(focusModifier),
                )
            }

            subtitle?.let { text ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    color = Tokens.Color.colorGrey400,
                )
            }
        }
    }
}
