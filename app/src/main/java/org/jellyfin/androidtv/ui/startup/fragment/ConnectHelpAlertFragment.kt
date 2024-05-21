package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
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
    }
}
