package org.jellyfin.androidtv.ui.settings.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsColumn(content: LazyListScope.() -> Unit) = LazyColumn(
	modifier = Modifier
		.padding(6.dp),
	verticalArrangement = Arrangement.spacedBy(4.dp),
	content = content,
)
