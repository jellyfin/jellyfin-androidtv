package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.composable.overscan
import org.jellyfin.androidtv.ui.shared.theme.JellyfinMaterialTheme

class ConnectHelpAlertFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        ConnectHelpAlert()
    }

    @Composable
    fun ConnectHelpAlert() {
        val focusRequester = FocusRequester()

        JellyfinMaterialTheme {
            Surface(
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .overscan(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        modifier = Modifier
                            .width(400.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = stringResource(R.string.login_help_title),
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text = stringResource(R.string.login_help_description),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(modifier = Modifier.padding(top = 24.dp).align(Alignment.Start)
                            .focusRequester(focusRequester).focusable(), onClick = {
                            parentFragmentManager.popBackStack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                text = stringResource(id = R.string.btn_got_it),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Image(
                        painter = painterResource(R.drawable.qr_jellyfin_docs),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .width(200.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}
