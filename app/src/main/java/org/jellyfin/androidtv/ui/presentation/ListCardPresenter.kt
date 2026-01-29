package org.jellyfin.androidtv.ui.presentation

import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.unit.dp
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.ui.composable.item.MovieListItem
import org.jellyfin.androidtv.ui.composable.item.MovieListItemCompact
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem

/**
 * A presenter that displays items in a horizontal list format with poster on the left
 * and title/metadata on the right. This provides an alternative to the grid card view
 * for browsing movies.
 *
 * @param compact Whether to use the compact layout (smaller poster, less metadata)
 * @param posterHeight The height of the poster in dp
 */
class ListCardPresenter(
    private val compact: Boolean = false,
    private val posterHeight: Int = if (compact) 80 else 120,
) : Presenter() {

    constructor() : this(false, 120)

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = ComposeView(parent.context).apply {
            setParentCompositionContext(parent.findViewTreeCompositionContext())
            setViewTreeLifecycleOwner(parent.findViewTreeLifecycleOwner())
            setViewTreeSavedStateRegistryOwner(parent.findViewTreeSavedStateRegistryOwner())
        }

        return ListCardViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        if (viewHolder !is ListCardViewHolder) return
        if (item !is BaseRowItem) return

        viewHolder.bind(item)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        if (viewHolder !is ListCardViewHolder) return

        viewHolder.unbind()
    }

    private inner class ListCardViewHolder(composeView: ComposeView) : ViewHolder(composeView) {
        private val _item = MutableStateFlow<BaseRowItem?>(null)
        private val _focused = MutableStateFlow(false)

        init {
            composeView.setContent {
                val item by _item.collectAsState()
                val focused by _focused.collectAsState()

                if (compact) {
                    MovieListItemCompact(
                        item = item,
                        focused = focused,
                        posterHeight = posterHeight.dp,
                    )
                } else {
                    MovieListItem(
                        item = item,
                        focused = focused,
                        posterHeight = posterHeight.dp,
                    )
                }
            }

            _focused.value = view.isFocused
            composeView.onFocusChangeListener = { _, focused -> _focused.value = focused }
        }

        fun bind(item: BaseRowItem) {
            _item.value = item
            _focused.value = view.isFocused
        }

        fun unbind() {
            _item.value = null
            _focused.value = false
        }
    }
}
