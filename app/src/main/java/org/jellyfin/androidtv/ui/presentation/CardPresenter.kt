package org.jellyfin.androidtv.ui.presentation

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.Image
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.item.ItemCard
import org.jellyfin.androidtv.ui.composable.item.ItemCardBaseItemOverlay
import org.jellyfin.androidtv.ui.composable.item.ItemPreview
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.BaseRowType
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem
import org.jellyfin.androidtv.util.BlurHashDecoder
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getUrl
import org.jellyfin.androidtv.util.getActivity
import org.jellyfin.design.Tokens
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private const val IMAGE_CROSSFADE_DURATION_MS = 100
private const val BLUR_HASH_RESOLUTION = 32

class CardPresenter(
	val showInfo: Boolean,
	val imageType: ImageType,
	val staticHeight: Int,
	val uniformAspect: Boolean,
) : Presenter() {
	constructor(showInfo: Boolean, imageType: ImageType, staticHeight: Int) : this(showInfo, imageType, staticHeight, false)
	constructor(showInfo: Boolean, staticHeight: Int) : this(showInfo, ImageType.POSTER, staticHeight)
	constructor(showInfo: Boolean) : this(showInfo, 150)
	constructor() : this(true)

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = ComposeView(parent.context).apply {
			setParentCompositionContext(parent.findViewTreeCompositionContext())
			setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
			setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
			isFocusable = true
			isFocusableInTouchMode = true

			setOnLongClickListener {
				context.getActivity()?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)) ?: false
			}
		}

		return CardViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		if (viewHolder !is CardViewHolder) return
		if (item !is BaseRowItem) return

		viewHolder.bind(item)
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		if (viewHolder !is CardViewHolder) return

		viewHolder.unbind()
	}

	private inner class CardViewHolder(composeView: ComposeView) : ViewHolder(composeView) {
		private val _item = MutableStateFlow<BaseRowItem?>(null)
		private val focused = mutableStateOf(false)

		init {
			composeView.setContent {
				val item by _item.collectAsState()

				CardViewHolderContent(
					item = item,
					focused = focused,
					showInfo = showInfo,
					imageType = imageType,
					staticHeight = staticHeight,
					uniformAspect = uniformAspect,
				)
			}

			focused.value = view.isFocused
			composeView.onFocusChangeListener = { _, isFocused -> focused.value = isFocused }
		}

		fun bind(item: BaseRowItem) {
			_item.value = item
			focused.value = view.isFocused
		}

		fun unbind() {
			_item.value = null
			focused.value = false
		}
	}
}

private data class BaseRowItemDisplayConfig(
	val image: JellyfinImage?,
	val iconRes: Int,
	val aspectRatio: Float,
	val overrideShowInfo: Boolean? = null,
	val scaleType: ImageView.ScaleType? = null,
)

private fun ImageView.ScaleType.toContentScale() = when (this) {
	ImageView.ScaleType.CENTER_CROP -> ContentScale.Crop
	ImageView.ScaleType.FIT_XY -> ContentScale.FillBounds
	ImageView.ScaleType.CENTER -> ContentScale.None
	ImageView.ScaleType.CENTER_INSIDE,
	ImageView.ScaleType.FIT_CENTER,
	ImageView.ScaleType.FIT_START,
	ImageView.ScaleType.FIT_END,
	ImageView.ScaleType.MATRIX -> ContentScale.Fit
}

@Composable
private fun rememberBlurHashPainter(
	blurHash: String?,
	aspectRatio: Float,
): Painter? {
	val bitmap by produceState<android.graphics.Bitmap?>(null, blurHash, aspectRatio) {
		value = if (blurHash == null || aspectRatio <= 0f) null else withContext(Dispatchers.Default) {
			BlurHashDecoder.decode(
				blurHash = blurHash,
				width = if (aspectRatio > 1f) (BLUR_HASH_RESOLUTION * aspectRatio).roundToInt() else BLUR_HASH_RESOLUTION,
				height = if (aspectRatio >= 1f) BLUR_HASH_RESOLUTION else (BLUR_HASH_RESOLUTION / aspectRatio).roundToInt(),
			)
		}
	}

	return remember(bitmap) { bitmap?.let { BitmapPainter(it.asImageBitmap()) } }
}

private fun BaseRowItem.getDisplayConfig(imageType: ImageType, uniformAspect: Boolean): BaseRowItemDisplayConfig = when (baseRowType) {
	BaseRowType.BaseItem -> {
		val preferSeriesPoster = this is BaseItemDtoBaseRowItem && preferSeriesPoster
		val primaryAspectRatio = baseItem?.primaryImageAspectRatio?.toFloat()
		val defaultAspectRatio = when {
			preferParentThumb && (baseItem?.parentThumbItemId != null || baseItem?.seriesThumbImageTag != null) -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			baseItem?.type == BaseItemKind.EPISODE && primaryAspectRatio != null -> primaryAspectRatio
			baseItem?.type == BaseItemKind.EPISODE && (baseItem.parentThumbItemId != null || baseItem.seriesThumbImageTag != null) -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			baseItem?.type == BaseItemKind.USER_VIEW -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> primaryAspectRatio ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		}

		val base = BaseRowItemDisplayConfig(
			aspectRatio = when (imageType) {
				ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
				ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
				else -> defaultAspectRatio
			},
			image = getImage(imageType),
			iconRes = R.drawable.ic_clapperboard,
		)

		when (baseItem?.type) {
			BaseItemKind.AUDIO, BaseItemKind.MUSIC_ALBUM -> base.copy(
				iconRes = R.drawable.ic_music_album,
				aspectRatio = if (uniformAspect || base.aspectRatio < 0.8f) 1f else base.aspectRatio,
			)

			BaseItemKind.PERSON,
			BaseItemKind.MUSIC_ARTIST -> base.copy(
				iconRes = R.drawable.ic_user,
				aspectRatio = if (uniformAspect || base.aspectRatio < 0.8f) 1f else base.aspectRatio,
			)

			BaseItemKind.SEASON, BaseItemKind.SERIES -> base.copy(
				aspectRatio = if (imageType == ImageType.POSTER) ImageHelper.ASPECT_RATIO_2_3.toFloat() else base.aspectRatio,
				iconRes = R.drawable.ic_tv
			)

			BaseItemKind.EPISODE -> when (preferSeriesPoster) {
				true -> base.copy(
					aspectRatio = ImageHelper.ASPECT_RATIO_2_3.toFloat(),
					iconRes = R.drawable.ic_tv
				)

				false -> base.copy(
					aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
					iconRes = R.drawable.ic_tv,
					overrideShowInfo = true,
				)
			}

			BaseItemKind.COLLECTION_FOLDER, BaseItemKind.USER_VIEW -> base.copy(
				aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
				iconRes = R.drawable.ic_folder,
			)

			BaseItemKind.FOLDER, BaseItemKind.GENRE, BaseItemKind.MUSIC_GENRE -> base.copy(
				iconRes = R.drawable.ic_folder,
			)

			BaseItemKind.PHOTO -> base.copy(
				iconRes = R.drawable.ic_photo
			)

			BaseItemKind.PHOTO_ALBUM, BaseItemKind.PLAYLIST -> base.copy(
				iconRes = R.drawable.ic_folder
			)

			BaseItemKind.MOVIE, BaseItemKind.VIDEO -> base.copy(
				aspectRatio = when (imageType) {
					ImageType.POSTER -> ImageHelper.ASPECT_RATIO_2_3.toFloat()
					else -> base.aspectRatio
				},
				iconRes = R.drawable.ic_clapperboard,
			)

			else -> base
		}
	}

	BaseRowType.LiveTvChannel -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: 1f
		},
		image = getImage(imageType),
		scaleType = ImageView.ScaleType.FIT_CENTER,
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.LiveTvProgram -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		},
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
		overrideShowInfo = true,
	)

	BaseRowType.LiveTvRecording -> BaseRowItemDisplayConfig(
		aspectRatio = when (imageType) {
			ImageType.BANNER -> ImageHelper.ASPECT_RATIO_BANNER.toFloat()
			ImageType.THUMB -> ImageHelper.ASPECT_RATIO_16_9.toFloat()
			else -> baseItem?.primaryImageAspectRatio?.toFloat() ?: ImageHelper.ASPECT_RATIO_7_9.toFloat()
		},
		image = getImage(imageType),
		iconRes = R.drawable.ic_tv,
	)

	BaseRowType.SeriesTimer -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		iconRes = R.drawable.ic_tv_timer,
		image = getImage(imageType),
		overrideShowInfo = true,
	)

	BaseRowType.Person -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_7_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_user,
	)

	BaseRowType.Chapter -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_16_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_clapperboard,
	)

	BaseRowType.GridButton -> BaseRowItemDisplayConfig(
		aspectRatio = ImageHelper.ASPECT_RATIO_7_9.toFloat(),
		image = getImage(imageType),
		iconRes = R.drawable.ic_clapperboard,
	)
}

@Composable
@Stable
private fun CardViewHolderContent(
	item: BaseRowItem?,
	focused: State<Boolean>,
	showInfo: Boolean,
	imageType: ImageType,
	staticHeight: Int,
	uniformAspect: Boolean,
) {
	val context = LocalContext.current
	val localDensity = LocalDensity.current

	val title = remember(item, context) { item?.getCardName(context) }
	val subtitle = remember(item, context) { item?.getSubText(context) }
	val displayConfig = remember(item, imageType, uniformAspect) { item?.getDisplayConfig(imageType, uniformAspect) }
	if (item == null || displayConfig == null) return

	val image = displayConfig.image
	val aspectRatio = displayConfig.aspectRatio.takeIf { it >= 0.1f }
		?: image?.aspectRatio?.takeIf { it >= 0.1f } ?: 1f

	val size = when (item.staticHeight) {
		true -> DpSize(staticHeight.dp * aspectRatio, staticHeight.dp)
		false if (aspectRatio > 1f) -> DpSize(130.dp * aspectRatio, 130.dp)
		else -> DpSize(150.dp * aspectRatio, 150.dp)
	}

	val usePreview = displayConfig.overrideShowInfo ?: showInfo

	val card = @Composable {
		ItemCard(
			image = {
				if (image != null) {
					val api = koinInject<ApiClient>()
					val imageUrl = remember(image, api, size, localDensity) {
						image.getUrl(
							api,
							maxWidth = with(localDensity) { size.width.roundToPx() },
							maxHeight = with(localDensity) { size.height.roundToPx() },
						)
					}
					val contentScale = (displayConfig.scaleType ?: ImageView.ScaleType.CENTER_CROP).toContentScale()
					val placeholder = rememberBlurHashPainter(image.blurHash, aspectRatio)
					val request = remember(imageUrl, context) {
						ImageRequest.Builder(context)
							.data(imageUrl)
							.crossfade(IMAGE_CROSSFADE_DURATION_MS)
							.build()
					}
					Image(
						painter = rememberAsyncImagePainter(
							model = request,
							placeholder = placeholder,
							contentScale = contentScale,
						),
						contentDescription = null,
						contentScale = contentScale,
						modifier = Modifier.fillMaxSize(),
					)
				} else if (item is GridButtonBaseRowItem && item.gridButton.imageRes != null) {
					Image(
						painter = painterResource(item.gridButton.imageRes),
						contentDescription = null,
						modifier = Modifier
							.fillMaxSize()
					)
				} else {
					Image(
						painter = painterResource(displayConfig.iconRes),
						contentDescription = null,
						modifier = Modifier
							.fillMaxSize(0.4f)
							.align(Alignment.Center)
					)
				}
			},
			overlay = {
				val showInfo = !usePreview && item.showCardInfoOverlay
				item.baseItem?.let { baseItem ->
					FocusAwareItemOverlay(
						item = baseItem,
						focused = focused,
						footer = {
							if (showInfo && title != null) {
								val focusModifier = focusedMarqueeModifier(focused)

								Box(
									modifier = Modifier
										.fillMaxWidth()
										.background(Tokens.Color.colorBluegrey900.copy(alpha = 0.6f), JellyfinTheme.shapes.extraSmall),
								) {
									Text(
										text = title,
										maxLines = 1,
										overflow = TextOverflow.Ellipsis,
										textAlign = TextAlign.Center,
										color = Tokens.Color.colorWhite,
										modifier = Modifier
											.then(focusModifier)
											.padding(Tokens.Space.spaceXs),
									)
								}
							}
						}
					)
				}
			},
			modifier = Modifier
				.size(size)
		)
	}

	if (usePreview) {
		ItemPreview(
			card = { card() },
			title = title?.let { text ->
				{
					Text(
						text = text,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						textAlign = TextAlign.Center,
						modifier = Modifier.then(focusedMarqueeModifier(focused)),
					)
				}
			},
			subtitle = subtitle?.let { text ->
				{
					Text(
						text = text,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						textAlign = TextAlign.Center,
						modifier = Modifier.then(focusedMarqueeModifier(focused)),
					)
				}
			},
		)
	} else {
		card()
	}
}

@Composable
private fun focusedMarqueeModifier(focused: State<Boolean>): Modifier = if (focused.value) Modifier.basicMarquee(
	iterations = Int.MAX_VALUE,
	initialDelayMillis = 0,
) else Modifier

@Composable
private fun FocusAwareItemOverlay(
	item: org.jellyfin.sdk.model.api.BaseItemDto,
	focused: State<Boolean>,
	footer: (@Composable () -> Unit)? = null,
) {
	ItemCardBaseItemOverlay(
		item = item,
		focused = focused.value,
		footer = footer,
	)
}
