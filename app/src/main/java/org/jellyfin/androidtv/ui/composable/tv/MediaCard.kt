package org.jellyfin.androidtv.ui.composable.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.ui.theme.JellyfinColors
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * TV-optimized media card component for displaying movies, TV shows, etc.
 * Uses TV-specific focus handling and Material 3 theming.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    item: BaseItemDto,
    modifier: Modifier = Modifier,
    width: Dp = 160.dp,
    aspectRatio: Float = 2f / 3f, // Default poster aspect ratio
    showTitle: Boolean = true,
    imageUrl: String? = null,
    onClick: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .aspectRatio(aspectRatio)
            .onFocusChanged { isFocused = it.isFocused },
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = if (isFocused) JellyfinColors.FocusedContainer else Color.Transparent,
        ),
        scale = androidx.tv.material3.CardDefaults.scale(
            focusedScale = 1.1f,
            pressedScale = 0.95f
        ),
        border = androidx.tv.material3.CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, JellyfinColors.Primary),
                shape = RoundedCornerShape(8.dp)
            )
        ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background image
            AsyncImage(
                model = imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay for title text
            if (showTitle && !item.name.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    JellyfinColors.MediaCardGradientEnd,
                                    JellyfinColors.MediaCardGradientStart
                                )
                            )
                        )
                )
                
                Text(
                    text = item.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * Simplified media card for grid layouts
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SimpleMediaCard(
    item: BaseItemDto,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    imageUrl: String? = null,
    onClick: () -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .onFocusChanged { isFocused = it.isFocused },
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = Color.Transparent,
        ),
        scale = androidx.tv.material3.CardDefaults.scale(
            focusedScale = 1.1f,
            pressedScale = 0.95f
        ),
        border = androidx.tv.material3.CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, JellyfinColors.Primary),
                shape = RoundedCornerShape(8.dp)
            )
        ),
        shape = androidx.tv.material3.CardDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = item.name,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}
