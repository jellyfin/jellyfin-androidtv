package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.LocalTextStyle
import org.jellyfin.androidtv.ui.base.ProvideTextStyle
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.composable.modifier.overscan

@Composable
private fun ConnectHelpAlert(
	onClose: () -> Unit,
) {
	val focusRequester = remember { FocusRequester() }

	Box(
		modifier = Modifier.background(colorResource(R.color.not_quite_black))
	) {
		ProvideTextStyle(LocalTextStyle.current.copy(color = colorResource(R.color.white))) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight()
					.overscan(),
				horizontalArrangement = Arrangement.SpaceEvenly,
			) {
				Column(
					modifier = Modifier
						.width(400.dp)
						.align(Alignment.CenterVertically),
				) {
					Text(
						text = stringResource(R.string.login_help_title),
						style = LocalTextStyle.current.copy(fontSize = 45.sp),
					)
					Text(
						modifier = Modifier.padding(top = 16.dp),
						text = stringResource(R.string.login_help_description),
						style = LocalTextStyle.current.copy(fontSize = 16.sp),
					)
					Button(
						modifier = Modifier
							.padding(top = 24.dp)
							.align(Alignment.Start)
							.focusRequester(focusRequester),
						onClick = onClose,
					) {
						Icon(
							imageVector = ImageVector.vectorResource(R.drawable.ic_check),
							contentDescription = null,
							modifier = Modifier.size(20.dp),
						)
						Spacer(Modifier.size(8.dp))
						Text(
							text = stringResource(id = R.string.btn_got_it),
							style = LocalTextStyle.current.copy(fontSize = 16.sp),
						)
					}
				}

				Image(
					painter = painterResource(R.drawable.qr_jellyfin_docs),
					contentDescription = stringResource(R.string.app_name),
					modifier = Modifier
						.width(200.dp)
						.align(Alignment.CenterVertically),
				)
			}
		}
	}

	LaunchedEffect(focusRequester) {
		focusRequester.requestFocus()
	}
}

class ConnectHelpAlertFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		JellyfinTheme {
			ConnectHelpAlert(
				onClose = { parentFragmentManager.popBackStack() },
			)
		}
	}
}
