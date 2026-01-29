package org.jellyfin.androidtv.constant

import org.jellyfin.androidtv.R

/**
 * Defines the view modes for displaying media items.
 * - GRID: Traditional grid/card view with posters
 * - LIST: Horizontal list view with poster on left and title on right
 */
enum class ViewMode(val nameRes: Int) {
    /**
     * Traditional grid view displaying poster cards
     */
    GRID(R.string.view_mode_grid),

    /**
     * List view with poster on left, title and metadata on right
     * Items are displayed in a horizontal scrollable list
     */
    LIST(R.string.view_mode_list),
}
