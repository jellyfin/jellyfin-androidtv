package org.jellyfin.androidtv.ui.composable.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import org.jellyfin.androidtv.ui.theme.JellyfinColors

/**
 * TV-optimized button component with focus handling
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: androidx.tv.material3.ButtonColors = ButtonDefaults.colors(
        containerColor = JellyfinColors.Primary,
        contentColor = Color.White,
        focusedContainerColor = JellyfinColors.PrimaryVariant,
        focusedContentColor = Color.White
    )
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        scale = ButtonDefaults.scale(
            focusedScale = 1.05f
        ),        border = ButtonDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = BorderStroke(2.dp, Color.White)
            )
        ),
        shape = ButtonDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        ),
        modifier = modifier.onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Action button row for media details
 */
@Composable
fun MediaActionButtons(
    onPlayClick: () -> Unit,
    onResumeClick: (() -> Unit)? = null,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean = false,
    canResume: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Primary play/resume button
        if (canResume && onResumeClick != null) {
            TvButton(
                text = "Resume",
                icon = Icons.Default.PlayArrow,
                onClick = onResumeClick
            )
            TvButton(
                text = "Play from Beginning",
                onClick = onPlayClick,
                colors = ButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    focusedContainerColor = JellyfinColors.Surface,
                    focusedContentColor = Color.White
                )
            )
        } else {
            TvButton(
                text = "Play",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayClick
            )
        }
        
        // Favorite button
        TvButton(
            text = if (isFavorite) "Unfavorite" else "Favorite",
            icon = Icons.Default.Star,
            onClick = onFavoriteClick,
            colors = ButtonDefaults.colors(
                containerColor = if (isFavorite) JellyfinColors.Secondary else Color.Transparent,
                contentColor = Color.White,
                focusedContainerColor = if (isFavorite) JellyfinColors.SecondaryVariant else JellyfinColors.Surface,
                focusedContentColor = Color.White
            )
        )
    }
}

/**
 * Information chip component for metadata display
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0x33FFFFFF),
    textColor: Color = Color.White
) {
    androidx.tv.material3.Card(
        onClick = { },
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = backgroundColor
        ),
        shape = androidx.tv.material3.CardDefaults.shape(
            shape = RoundedCornerShape(16.dp)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Rating display component
 */
@Composable
fun RatingDisplay(
    rating: Float,
    maxRating: Float = 10f,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = JellyfinColors.Accent1,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
        Text(
            text = "/ ${maxRating.toInt()}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
