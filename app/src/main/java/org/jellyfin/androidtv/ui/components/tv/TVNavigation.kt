package org.jellyfin.androidtv.ui.components.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import coil3.compose.AsyncImage
import org.jellyfin.androidtv.ui.theme.JellyfinTheme

/**
 * Navigation tab item data
 */
data class NavigationItem(
    val id: String,
    val title: String,
    val icon: Any, // Can be ImageVector or drawable resource ID
    val contentDescription: String? = null,
)

/**
 * TV-optimized navigation bar replacing Leanback's browse headers
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
    ) {
        items.forEachIndexed { index, item ->            Tab(
                selected = selectedIndex == index,
                onFocus = { onItemSelected(index) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (val iconData = item.icon) {
                        is ImageVector -> {
                            Icon(
                                imageVector = iconData,
                                contentDescription = item.contentDescription,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        is Int -> {
                            Icon(
                                painter = painterResource(iconData),
                                contentDescription = item.contentDescription,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * TV-optimized toolbar with user profile and action buttons
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVToolbar(
    title: String? = null,
    userImageUrl: String? = null,
    userName: String? = null,
    onUserClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title section
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search button
            TVIconButton(
                onClick = onSearchClick,
                icon = android.R.drawable.ic_search_category_default,
                contentDescription = "Search"
            )
            
            // Settings button  
            TVIconButton(
                onClick = onSettingsClick,
                icon = android.R.drawable.ic_menu_preferences,
                contentDescription = "Settings"
            )
            
            // User profile
            TVIconButton(
                onClick = onUserClick,
                contentDescription = userName ?: "User profile"
            ) {
                if (userImageUrl != null) {
                    AsyncImage(
                        model = userImageUrl,
                        contentDescription = userName,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName?.take(1) ?: "U",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * TV-optimized icon button component
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    imageVector: ImageVector? = null,
    contentDescription: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val tvColors = JellyfinTheme.tvColors
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .focusable(),
        interactionSource = interactionSource,        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = ClickableSurfaceDefaults.shape(
            shape = CircleShape,
            focusedShape = CircleShape,
        ),        border = ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    2.dp, 
                    MaterialTheme.colorScheme.primary
                )
            )
        )
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {            when {
                content != null -> content()
                imageVector != null -> Icon(
                    imageVector = imageVector,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                icon != null -> Icon(
                    painter = painterResource(icon),
                    contentDescription = contentDescription,
                    modifier = Modifier.size(24.dp),
                    tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
