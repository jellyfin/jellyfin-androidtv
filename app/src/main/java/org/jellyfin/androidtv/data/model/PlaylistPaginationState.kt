/**
 * Playlist Pagination State Management
 *
 * This file was added as part of the playlist pagination enhancement to support
 * browsing large playlists with proper item numbering (1-100, 101-200, etc.)
 * and traditional page navigation with Previous/Next buttons.
 *
 * Key features:
 * - 100 items per page with 1-based indexing
 * - Proper item numbering across pages
 * - Loading state management
 * - Page calculation helpers
 */
package org.jellyfin.androidtv.data.model

import kotlin.jvm.JvmName

/**
 * Manages pagination state for playlist items
 */
data class PlaylistPaginationState(
    @get:JvmName("getCurrentPage")
    val currentPage: Int = 1, // 1-based indexing for user-friendly display

    @get:JvmName("getTotalItems")
    val totalItems: Int = 0,

    @get:JvmName("getPageSize")
    val pageSize: Int = 100,

    @get:JvmName("getIsLoading")
    val isLoading: Boolean = false
) {
    val totalPages: Int
        get() = if (totalItems == 0) 1 else (totalItems + pageSize - 1) / pageSize

    val startIndex: Int
        get() = (currentPage - 1) * pageSize

    @get:JvmName("hasNextPage")
    val hasNextPage: Boolean
        get() = currentPage < totalPages

    @get:JvmName("hasPreviousPage")
    val hasPreviousPage: Boolean
        get() = currentPage > 1

    @get:JvmName("isFirstPage")
    val isFirstPage: Boolean
        get() = currentPage == 1

    @get:JvmName("isLastPage")
    val isLastPage: Boolean
        get() = currentPage == totalPages || totalItems == 0

    // isLoading property is already a constructor parameter, no need for custom getter

    /**
     * Creates a new state with updated page
     */
    @JvmName("withPage")
    fun withPage(page: Int): PlaylistPaginationState {
        return copy(currentPage = page.coerceIn(1, totalPages))
    }

    /**
     * Creates a new state with loading status
     */
    @JvmName("withLoading")
    fun withLoading(loading: Boolean): PlaylistPaginationState {
        return copy(isLoading = loading)
    }

    /**
     * Creates a new state with updated total items count
     */
    @JvmName("withTotalItems")
    fun withTotalItems(total: Int): PlaylistPaginationState {
        return copy(totalItems = total)
    }

    /**
     * Get display text for current page info
     */
    @JvmName("getPageDisplayText")
    fun getPageDisplayText(): String {
        return if (totalItems == 0) {
            "Page 1 of 1"
        } else {
            val startItem = startIndex + 1
            val endItem = (startIndex + pageSize).coerceAtMost(totalItems)
            "Page $currentPage of $totalPages ($startItem-$endItem of $totalItems items)"
        }
    }
}