package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.player.base.toast.MediaToast

class SkipIndicatorView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0,
) : AbstractComposeView(context, attrs, defStyle) {

	data class SkipIndicatorState(val forward: Boolean, val seconds: Int)

	private val _state = MutableStateFlow<SkipIndicatorState?>(null)
	private val _showCount = MutableStateFlow(0)

	fun show(forward: Boolean, seconds: Int) {
		_state.value = SkipIndicatorState(forward, seconds)
		_showCount.value++
	}

	@Composable
	override fun Content() {
		val state by _state.collectAsState()
		val showCount by _showCount.collectAsState()

		LaunchedEffect(showCount) {
			if (showCount > 0) {
				delay(700)
				_state.value = null
			}
		}

		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
			MediaToast(
				visible = state != null,
				icon = {
					val s = state ?: return@MediaToast
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center,
					) {
						Icon(
							imageVector = ImageVector.vectorResource(
								if (s.forward) R.drawable.ic_fast_forward else R.drawable.ic_rewind
							),
							contentDescription = null,
							modifier = Modifier.size(32.dp),
						)
						Text(
							text = "${s.seconds}s",
							fontSize = 12.sp,
						)
					}
				},
			)
		}
	}
}
