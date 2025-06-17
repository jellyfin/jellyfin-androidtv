package org.jellyfin.androidtv.ui.composable.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * TV-optimized horizontal scrolling row of media items
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaRow(
    title: String,
    items: List<BaseItemDto>,
    modifier: Modifier = Modifier,
    onItemClick: (BaseItemDto) -> Unit = {},
    onItemFocus: (BaseItemDto) -> Unit = {},
    getItemImageUrl: (BaseItemDto) -> String? = { null }
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
          LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                MediaCard(
                    item = item,
                    imageUrl = getItemImageUrl(item),
                    onClick = { onItemClick(item) },
                    modifier = Modifier.onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onItemFocus(item)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Main browse layout with multiple media rows
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaBrowseLayout(
    sections: List<MediaSection>,
    modifier: Modifier = Modifier,
    onItemClick: (BaseItemDto) -> Unit = {},
    onItemFocus: (BaseItemDto) -> Unit = {},
    getItemImageUrl: (BaseItemDto) -> String? = { null }
) {    LazyColumn(
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = modifier
    ) {
        items(sections) { section ->
            MediaRow(
                title = section.title,
                items = section.items,
                onItemClick = onItemClick,
                onItemFocus = onItemFocus,
                getItemImageUrl = getItemImageUrl
            )
        }
    }
}

/**
 * Data class for organizing media sections
 */
data class MediaSection(
    val title: String,
    val items: List<BaseItemDto>
)
