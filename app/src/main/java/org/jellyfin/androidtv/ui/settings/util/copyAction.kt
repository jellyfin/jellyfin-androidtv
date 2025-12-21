package org.jellyfin.androidtv.ui.settings.util

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R

@Composable
fun copyAction(data: ClipData): () -> Unit {
	val context = LocalContext.current
	val clipboard = LocalClipboard.current
	val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope

	return {
		lifecycleScope.launch {
			clipboard.setClipEntry(ClipEntry(data))

			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
				Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
			}
		}
	}
}
