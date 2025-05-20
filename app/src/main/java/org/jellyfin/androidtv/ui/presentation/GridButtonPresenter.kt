package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.itemhandling.GridButtonBaseRowItem

class GridButtonPresenter @JvmOverloads constructor(
	private val width: Int = 110,
	private val imageHeight: Int = 110,
) : Presenter() {
	private class ComposeViewWrapper(composeView: ComposeView) : FrameLayout(composeView.context) {
		init {
			isFocusable = true
			isFocusableInTouchMode = true
			descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
			addView(composeView)
		}

		// Hack to prevent Compose crash with leanback presenters
		override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
			if (isAttachedToWindow) super.onMeasure(widthMeasureSpec, heightMeasureSpec)
			else setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
		}
	}

	inner class ViewHolder(
		private val composeView: ComposeView,
	) : Presenter.ViewHolder(ComposeViewWrapper(composeView)) {
		fun bind(value: GridButton) = composeView.setContent {
			Box(
				modifier = Modifier
					.width(width.dp)
					.clip(RoundedCornerShape(4.dp))
					.background(colorResource(R.color.button_default_normal_background))
			) {
				if (value.imageRes != null) {
					Image(
						painter = painterResource(value.imageRes),
						contentDescription = value.text,
						contentScale = ContentScale.Crop,
						modifier = Modifier.size(width.dp, imageHeight.dp)
					)
				}

				Text(
					text = value.text,
					style = TextStyle(
						color = colorResource(R.color.button_default_normal_text),
						fontSize = 12.sp
					),
					modifier = Modifier
						.padding(15.dp, 10.dp)
						.align(Alignment.BottomStart)
				)
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
		ViewHolder(ComposeView(parent.context))

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
		if (viewHolder !is ViewHolder) return

		when (item) {
			is GridButtonBaseRowItem -> viewHolder.bind(item.gridButton)
			is GridButton -> viewHolder.bind(item)
		}
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit
	override fun onViewAttachedToWindow(viewHolder: Presenter.ViewHolder) = Unit
}
