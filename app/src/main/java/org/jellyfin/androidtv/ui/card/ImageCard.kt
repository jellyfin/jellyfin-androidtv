package org.jellyfin.androidtv.ui.card

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.composable.AsyncImage
import java.text.NumberFormat

@Composable
fun ImageCard(
	modifier: Modifier,
	showInfo: Boolean = true,
	showInfoOverlay: Boolean = false,
	mainImageUrl: String? = null,
	placeholder: Drawable? = null,
	defaultIconRes: Int? = null,
	overlayIconRes: Int? = null,
	overlayText: String? = null,
	overlayCount: String? = null,
	aspectRatio: Float = 2f / 3f,
	scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
	title: String? = null,
	contentText: String? = null,
	rating: String? = null,
	unwatchedCount: Int = -1,
	progress: Int = 0,
	isPlaying: Boolean = false,
	isFavorite: Boolean = false,
	onClick: () -> Unit = {},
	onLongClick: () -> Unit = {},
	onFocus: (isFocused: Boolean) -> Unit = {}
) {
	val nf = remember { NumberFormat.getInstance() }
	val interactionSource = remember { MutableInteractionSource() }

	val isFocused by interactionSource.collectIsFocusedAsState()
	val borderColor = if (isFocused) {
		JellyfinTheme.colorScheme.onInputFocused
	} else {
		Color.Transparent
	}
	val cardShape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
			.onFocusChanged { onFocus(it.isFocused) }
			.combinedClickable(
				onClick = onClick,
				onLongClick = onLongClick,
				indication = null,
				interactionSource = interactionSource,
			)
			.focusable(interactionSource = interactionSource)

    ) {
        Column {
            Box(
                modifier = Modifier
					.fillMaxWidth()
					.border(3.dp, borderColor, cardShape)
					.clip(cardShape)
            ) {
                // Main Image
				if (mainImageUrl != null) {
					AsyncImage(
						url = mainImageUrl,
						scaleType = scaleType,
						modifier = Modifier
							.fillMaxWidth()
							.aspectRatio(aspectRatio)

					)
				}
				else {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.aspectRatio(aspectRatio)
							.background(JellyfinTheme.colorScheme.itemFill),
						contentAlignment = Alignment.Center
					) {
						defaultIconRes?.let {
							Image(
								painter = painterResource(defaultIconRes),
								contentDescription = null,
								modifier = Modifier.size(48.dp)
							)
						}
					}
				}

                // Overlay
                if (!showInfo && showInfoOverlay) {
                    Box(
                        modifier = Modifier
							.align(Alignment.BottomCenter)
							.fillMaxWidth()
							.background(Color.Black.copy(alpha = 0.7f))
							.padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
							overlayIconRes?.let{
								Image(
									painter = painterResource(overlayIconRes),
									contentDescription = null,
									modifier = Modifier.size(24.dp)
								)
								Spacer(modifier = Modifier.width(8.dp))
							}

                            overlayText?.let{
								Text(
									text = overlayText,
									color = Color.White,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis
								)
							}

							overlayCount?.let {
                                Text(
                                    text = overlayCount,
                                    color = Color.White,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Progress bar
                if (progress > 0) {
                    Box(
                        modifier = Modifier
							.align(Alignment.BottomCenter)
							.fillMaxWidth()
							.height(4.dp)
							.background(Color.Gray)
                    ) {
                        Box(
                            modifier = Modifier
								.fillMaxHeight()
								.fillMaxWidth(progress / 100f)
								.background(Color.White)
                        )
                    }
                }

                // Watched indicator
                if (unwatchedCount >= 0) {
                    Box(
                        modifier = Modifier
							.align(Alignment.TopEnd)
							.padding(4.dp)
                    ) {
                        if (unwatchedCount > 0) {
                            Text(
                                text = if (unwatchedCount > 99)
                                    stringResource(R.string.watch_count_overflow)
                                else
                                    nf.format(unwatchedCount),
                                color = Color.White,
                                modifier = Modifier
									.background(
										Color.Black.copy(alpha = 0.7f),
										RoundedCornerShape(4.dp)
									)
									.padding(4.dp)
                            )
                        } else if (unwatchedCount == 0) {
                            Image(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Playing indicator
                if (isPlaying) {
                    Image(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        modifier = Modifier
							.align(Alignment.TopStart)
							.padding(4.dp)
							.size(24.dp)
                    )
                }

                // Favorite icon
                if (isFavorite) {
                    Image(
                        painter = painterResource(R.drawable.ic_heart_red),
                        contentDescription = null,
                        modifier = Modifier
							.align(Alignment.TopStart)
							.padding(4.dp)
							.size(24.dp)
                    )
                }
            }

            // Info section
            if (showInfo) {
                Column(
                    modifier = Modifier
						.fillMaxWidth()
						.padding(2.dp, vertical = 6.dp)
                ) {
                    title?.let {
                        Text(
                            text = it,
                            maxLines = if (contentText.isNullOrEmpty()) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
							color = Color.White,
							fontSize = 11.sp
                        )
                    }

                    contentText?.let {
                        Text(
                            text = it,
                            maxLines = if (title.isNullOrEmpty()) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
							color = Color.Gray,
							fontSize = 11.sp
                        )
                    }

                    rating?.let {
                        Text(
							modifier = Modifier.padding(top = 4.dp),
                            text = it,
							fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

}
