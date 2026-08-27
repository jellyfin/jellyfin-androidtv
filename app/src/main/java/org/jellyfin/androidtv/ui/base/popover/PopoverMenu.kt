package org.jellyfin.androidtv.ui.base.popover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults

@Composable
fun PopoverMenu(
	modifier: Modifier = Modifier,
	paddingValues: PaddingValues = PaddingValues(4.dp),
	content: @Composable ColumnScope.() -> Unit,
) = Column(
	modifier = Modifier
		.padding(paddingValues)
		.width(IntrinsicSize.Max)
		.then(modifier)
) {
	content()
}

@Composable
fun PopoverMenuItem(
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = RoundedCornerShape(3.dp),
	content: @Composable RowScope.() -> Unit,
) = Button(
	onClick = onClick,
	modifier = Modifier
		.fillMaxWidth()
		.then(modifier),
	shape = shape,
	colors = ButtonDefaults.colors(
		containerColor = Color.Transparent,
	),
) {
	content()
}

@Composable
fun PopoverMenuCheckboxItem(
	selected: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
	content: @Composable RowScope.() -> Unit,
) = PopoverMenuItem(
	onClick = onClick,
	modifier = modifier,
) {
	val alpha by animateFloatAsState(
		targetValue = if (selected) 1f else 0f,
		animationSpec = tween(durationMillis = 150),
	)

	Icon(
		imageVector = ImageVector.vectorResource(R.drawable.ic_check),
		contentDescription = null,
		modifier = Modifier
			.size(16.dp)
			.alpha(alpha),
	)

	Spacer(Modifier.width(16.dp))

	content()
}
