package org.jellyfin.androidtv.ui.composable.modifier

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Default overscan values of 48 horizontal and 27 vertical display pixels.
 */
val overscanPaddingValues = PaddingValues(48.dp, 27.dp)

/**
 * Apply a [padding] with [overscanPaddingValues].
 */
fun Modifier.overscan(): Modifier = padding(overscanPaddingValues)
