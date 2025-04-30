package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.composable.AsyncImage
import org.jellyfin.androidtv.ui.composable.rememberPlayerProgress
import org.jellyfin.androidtv.ui.composable.rememberQueueEntry
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.apiclient.albumPrimaryImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.apiclient.itemImages
import org.jellyfin.androidtv.util.apiclient.parentImages
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.jellyfin.queue.baseItem
import org.jellyfin.playback.jellyfin.queue.baseItemFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.ImageType
import org.koin.compose.koinInject

@Composable
fun NowPlayingComposable(
	onFocusableChange: (focusable: Boolean) -> Unit,
) {
	val api = koinInject<ApiClient>()
	val playbackManager = koinInject<PlaybackManager>()
	val navigationRepository = koinInject<NavigationRepository>()

	val entry by rememberQueueEntry(playbackManager)
	val item = entry?.run { baseItemFlow.collectAsState(baseItem) }?.value
	val progress = rememberPlayerProgress(playbackManager)

	LaunchedEffect(item == null) { onFocusableChange(item != null) }

	AnimatedContent(
		targetState = item,
		transitionSpec = { fadeIn() togetherWith fadeOut() },
	) { item ->
		if (item != null) {
			ButtonBase(
				onClick = { navigationRepository.navigate(Destinations.nowPlaying) },
				shape = JellyfinTheme.shapes.extraSmall,
				modifier = Modifier
					.widthIn(0.dp, 250.dp)
			) {
				Box(
					modifier = Modifier
						.align(Alignment.BottomStart)
						.fillMaxWidth()
						.height(1.dp)
						.drawWithContent {
							// Background
							drawRect(Color.White, alpha = 0.4f)
							// Foreground
							drawRect(Color.White, size = size.copy(width = progress * size.width))
						}
				)

				ProvideTextStyle(
					value = TextStyle.Default.copy(
						fontSize = 12.sp,
					)
				) {
					Row(
						horizontalArrangement = Arrangement.spacedBy(10.dp),
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier
							.padding(5.dp)
					) {
						val image = item.itemImages[ImageType.PRIMARY] ?: item.albumPrimaryImage ?: item.parentImages[ImageType.PRIMARY]

						AsyncImage(
							url = image?.getUrl(api),
							blurHash = image?.blurHash,
							placeholder = ContextCompat.getDrawable(LocalContext.current, R.drawable.ic_album),
							aspectRatio = image?.aspectRatio ?: 1f,
							modifier = Modifier
								.size(35.dp)
								.clip(RoundedCornerShape(4.dp)),
							scaleType = ImageView.ScaleType.CENTER_CROP,
						)

						Column(
							verticalArrangement = Arrangement.SpaceAround,
						) {
							// Name
							Text(text = item.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
							val artists = item.artists ?: item.albumArtists ?: item.albumArtist?.let(::listOf)
							Text(text = artists?.joinToString(", ").orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
						}
					}
				}
			}
		}
	}
}

class NowPlayingView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
	@Composable
	override fun Content() = NowPlayingComposable(
		// Workaround for older Android versions unable to find focus in our toolbar view when the NowPlayingView is added but inactive
		onFocusableChange = { focusable ->
			isFocusable = focusable
			descendantFocusability = if (focusable) FOCUS_AFTER_DESCENDANTS else FOCUS_BLOCK_DESCENDANTS
		}
	)
}
