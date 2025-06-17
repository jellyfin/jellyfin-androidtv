package org.jellyfin.androidtv.ui.composable.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * TV-optimized grid layout for browsing media items
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaGrid(
    title: String,
    items: List<BaseItemDto>,
    modifier: Modifier = Modifier,
    columns: Int = 6,
    onItemClick: (BaseItemDto) -> Unit = {},
    onItemFocus: (BaseItemDto) -> Unit = {},
    getItemImageUrl: (BaseItemDto) -> String? = { null }
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 16.dp)
        )
          LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                MediaCard(
                    item = item,
                    imageUrl = getItemImageUrl(item),
                    onClick = { onItemClick(item) },
                    showTitle = false, // Grid layout typically doesn't show titles on cards
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Compact grid for library tiles and shortcuts
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryTileGrid(
    items: List<BaseItemDto>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    onItemClick: (BaseItemDto) -> Unit = {},
    getItemImageUrl: (BaseItemDto) -> String? = { null }
) {    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(items) { item ->
            SimpleMediaCard(
                item = item,
                imageUrl = getItemImageUrl(item),
                onClick = { onItemClick(item) },
                size = 140.dp
            )
        }
    }
}
