package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import org.jellyfin.sdk.model.api.MediaSegmentType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SkipOverlayComposable(
	visible: Boolean,
	segmentType: MediaSegmentType?,
) {
	val shape = RoundedCornerShape(18.dp)
	val contentColor = Color.White
	val accentColor = colorResource(R.color.jellyfin_blue)

	Box(
		contentAlignment = Alignment.BottomEnd,
		modifier = Modifier.padding(horizontal = 56.dp, vertical = 44.dp)
	) {
		AnimatedVisibility(
			visible = visible,
			enter = fadeIn() + slideInHorizontally { it / 3 },
			exit = fadeOut() + slideOutHorizontally { it / 4 },
		) {
			Row(
				modifier = Modifier
					.shadow(14.dp, shape)
					.clip(shape)
					.background(colorResource(R.color.popup_menu_background).copy(alpha = 0.92f))
					.border(1.dp, contentColor.copy(alpha = 0.16f), shape)
					.padding(horizontal = 18.dp, vertical = 12.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier
						.size(34.dp)
						.clip(CircleShape)
						.background(accentColor.copy(alpha = 0.22f))
				) {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_next),
						contentDescription = null,
						modifier = Modifier.size(22.dp),
						tint = contentColor,
					)
				}

				Text(
					text = skipLabel(segmentType),
					color = contentColor,
					fontSize = 20.sp,
					fontWeight = FontWeight.SemiBold,
				)

				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier
						.size(30.dp)
						.clip(CircleShape)
						.background(contentColor.copy(alpha = 0.08f))
				) {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_control_select),
						contentDescription = stringResource(R.string.segment_skip_control_hint),
						modifier = Modifier.size(21.dp),
						tint = Color.Unspecified,
					)
				}
			}
		}
	}
}

@Composable
private fun skipLabel(segmentType: MediaSegmentType?) = when (segmentType) {
	MediaSegmentType.INTRO -> stringResource(R.string.segment_skip_intro)
	MediaSegmentType.OUTRO -> stringResource(R.string.segment_skip_outro)
	MediaSegmentType.RECAP -> stringResource(R.string.segment_skip_recap)
	MediaSegmentType.PREVIEW -> stringResource(R.string.segment_skip_preview)
	MediaSegmentType.COMMERCIAL -> stringResource(R.string.segment_skip_commercial)
	MediaSegmentType.UNKNOWN,
	null -> stringResource(R.string.segment_action_skip)
}

class SkipOverlayView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
	private val _currentPosition = MutableStateFlow(Duration.ZERO)
	private val _targetPosition = MutableStateFlow<Duration?>(null)
	private val _segmentType = MutableStateFlow<MediaSegmentType?>(null)
	private val _skipUiEnabled = MutableStateFlow(true)

	var currentPosition: Duration
		get() = _currentPosition.value
		set(value) {
			_currentPosition.value = value
		}

	var currentPositionMs: Long
		get() = _currentPosition.value.inWholeMilliseconds
		set(value) {
			_currentPosition.value = value.milliseconds
		}

	var targetPosition: Duration?
		get() = _targetPosition.value
		set(value) {
			_targetPosition.value = value
		}

	var targetPositionMs: Long?
		get() = _targetPosition.value?.inWholeMilliseconds
		set(value) {
			_targetPosition.value = value?.milliseconds
		}

	var segmentType: MediaSegmentType?
		get() = _segmentType.value
		set(value) {
			_segmentType.value = value
		}

	var skipUiEnabled: Boolean
		get() = _skipUiEnabled.value
		set(value) {
			_skipUiEnabled.value = value
		}

	val visible: Boolean
		get() {
			val enabled = _skipUiEnabled.value
			val targetPosition = _targetPosition.value
			val currentPosition = _currentPosition.value

			return enabled && targetPosition != null && currentPosition <= (targetPosition - MediaSegmentRepository.SkipMinDuration)
		}

	@Composable
	override fun Content() {
		val skipUiEnabled by _skipUiEnabled.collectAsState()
		val currentPosition by _currentPosition.collectAsState()
		val targetPosition by _targetPosition.collectAsState()
		val segmentType by _segmentType.collectAsState()

		val visible by remember(skipUiEnabled, currentPosition, targetPosition) {
			derivedStateOf { visible }
		}

		// Auto hide
		LaunchedEffect(skipUiEnabled, targetPosition) {
			delay(MediaSegmentRepository.AskToSkipAutoHideDuration)
			_targetPosition.value = null
			_segmentType.value = null
		}

		SkipOverlayComposable(visible, segmentType)
	}
}
