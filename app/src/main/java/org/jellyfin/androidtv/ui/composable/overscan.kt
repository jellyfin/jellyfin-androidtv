package org.jellyfin.androidtv.ui.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Apply a [padding] with the default overscan values of 48 horizontal and 27 vertical display pixels.
 */
fun Modifier.overscan(): Modifier = then(padding(48.dp, 27.dp))
