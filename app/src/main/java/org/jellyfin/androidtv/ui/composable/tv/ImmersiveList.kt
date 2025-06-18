package org.jellyfin.androidtv.ui.composable.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.ui.theme.JellyfinColors
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Layout type for the immersive list
 */
enum class ImmersiveListLayout {
    HORIZONTAL_CARDS,
    VERTICAL_GRID
}

/**
 * Background mode for the immersive list
 */
enum class BackgroundMode {
    FOCUSED_ITEM,
    STATIC,
    NONE
}

/**
 * Immersive List component optimized for TV interfaces
 * Features:
 * - Adaptive layouts (horizontal cards or vertical grid)
 * - Immersive background from focused item
 * - Smooth focus animations
 * - Proper D-pad navigation
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ImmersiveList(
    title: String,
    items: List<BaseItemDto>,
    modifier: Modifier = Modifier,
    layout: ImmersiveListLayout = ImmersiveListLayout.HORIZONTAL_CARDS,
    backgroundMode: BackgroundMode = BackgroundMode.FOCUSED_ITEM,
    onItemClick: (BaseItemDto) -> Unit = {},
    onItemFocus: (BaseItemDto) -> Unit = {},
    getItemImageUrl: (BaseItemDto) -> String? = { null },
    getItemBackdropUrl: (BaseItemDto) -> String? = { null }
) {    var focusedItem by remember { mutableStateOf<BaseItemDto?>(null) }
    
    // Manage background updates with debouncing
    LaunchedEffect(focusedItem) {
        if (focusedItem != null) {
            // Small delay to prevent rapid background changes during navigation
            delay(150)
            onItemFocus(focusedItem!!)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Immersive background
        when (backgroundMode) {
            BackgroundMode.FOCUSED_ITEM -> {
                ImmersiveBackground(
                    item = focusedItem,
                    getBackdropUrl = getItemBackdropUrl
                )
            }
            BackgroundMode.STATIC -> {
                // Could show a static library background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
            BackgroundMode.NONE -> {
                // No background
            }
        }
        
        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            when (layout) {
                ImmersiveListLayout.HORIZONTAL_CARDS -> {
                    HorizontalCardsList(
                        items = items,
                        onItemClick = onItemClick,
                        onItemFocus = { item -> focusedItem = item },
                        getItemImageUrl = getItemImageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ImmersiveListLayout.VERTICAL_GRID -> {
                    VerticalCardsGrid(
                        items = items,
                        onItemClick = onItemClick,
                        onItemFocus = { item -> focusedItem = item },
                        getItemImageUrl = getItemImageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Immersive background that shows backdrop of focused item
 */
@Composable
private fun ImmersiveBackground(
    item: BaseItemDto?,
    getBackdropUrl: (BaseItemDto) -> String?
) {
    AnimatedVisibility(
        visible = item != null,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(300))
    ) {
        if (item != null) {
            val backdropUrl = getBackdropUrl(item)
            if (backdropUrl != null) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.6f
                )
            }
        }
    }
}

/**
 * Horizontal cards layout for immersive list
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HorizontalCardsList(
    items: List<BaseItemDto>,
    onItemClick: (BaseItemDto) -> Unit,
    onItemFocus: (BaseItemDto) -> Unit,
    getItemImageUrl: (BaseItemDto) -> String?,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = modifier
    ) {
        items(items) { item ->
            ImmersiveListCard(
                item = item,
                onClick = { onItemClick(item) },
                onFocus = { onItemFocus(item) },
                imageUrl = getItemImageUrl(item),
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

/**
 * Vertical grid layout for immersive list
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VerticalCardsGrid(
    items: List<BaseItemDto>,
    onItemClick: (BaseItemDto) -> Unit,
    onItemFocus: (BaseItemDto) -> Unit,
    getItemImageUrl: (BaseItemDto) -> String?,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        items(items) { item ->
            ImmersiveListCard(
                item = item,
                onClick = { onItemClick(item) },
                onFocus = { onItemFocus(item) },
                imageUrl = getItemImageUrl(item),
                modifier = Modifier.aspectRatio(2f / 3f)
            )
        }
    }
}

/**
 * Card component for immersive list
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ImmersiveListCard(
    item: BaseItemDto,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocus()
                }
            },
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = if (isFocused) JellyfinColors.FocusedContainer else Color.Transparent,
        ),
        scale = androidx.tv.material3.CardDefaults.scale(
            focusedScale = 1.1f,
            pressedScale = 0.95f
        ),
        border = androidx.tv.material3.CardDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
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
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0.5f
                        )
                    )
            )
            
            // Title overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = item.name ?: "Unknown Title",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Additional metadata
                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Helper data class for immersive list sections
 */
data class ImmersiveListSection(
    val title: String,
    val items: List<BaseItemDto>,
    val layout: ImmersiveListLayout = ImmersiveListLayout.HORIZONTAL_CARDS
)

/**
 * Multi-section immersive list
 */
@Composable
fun MultiSectionImmersiveList(
    sections: List<ImmersiveListSection>,
    modifier: Modifier = Modifier,
    onItemClick: (BaseItemDto) -> Unit = {},
    onItemFocus: (BaseItemDto) -> Unit = {},
    getItemImageUrl: (BaseItemDto) -> String? = { null },
    getItemBackdropUrl: (BaseItemDto) -> String? = { null }
) {    var globalFocusedItem by remember { mutableStateOf<BaseItemDto?>(null) }
    
    LaunchedEffect(globalFocusedItem) {
        if (globalFocusedItem != null) {
            delay(150)
            onItemFocus(globalFocusedItem!!)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background
        ImmersiveBackground(
            item = globalFocusedItem,
            getBackdropUrl = getItemBackdropUrl
        )
        
        // Content
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        ) {
            items(sections) { section ->
                ImmersiveList(
                    title = section.title,
                    items = section.items,
                    layout = section.layout,
                    backgroundMode = BackgroundMode.NONE, // Background handled globally
                    onItemClick = onItemClick,
                    onItemFocus = { item -> globalFocusedItem = item },
                    getItemImageUrl = getItemImageUrl,
                    getItemBackdropUrl = getItemBackdropUrl,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
