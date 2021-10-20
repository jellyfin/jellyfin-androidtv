package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.LeanbackOverlayFragment
import org.jellyfin.androidtv.R

class AdjustPlaybackSpeedAction(
    context: Context,
    customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue
) : CustomAction(context, customPlaybackTransportControlGlue) {
    override fun handleClickAction(
        playbackController: PlaybackController,
        leanbackOverlayFragment: LeanbackOverlayFragment,
        context: Context,
        view: View
    ) {
        val speeds = sortedMapOf(
            Pair("0.25", .25f),
            Pair("0.5", .5f),
            Pair("0.75", .75f),
            Pair("1", 1f),
            Pair("1.25", 1.25f),
            Pair("1.5", 1.5f),
            Pair("1.75", 1.75f),
            Pair("2", 2f),
            Pair("2.5", 2.5f),
            Pair("3", 3f),
            Pair("4", 4f),
        )

        val popupMenu = PopupMenu(context, view, Gravity.TOP)
        val menu = popupMenu.menu
        speeds.forEach { speed ->
            val item = menu.add(speed.component1())
            item.isCheckable = true
            if (playbackController.playbackSpeed == speed.component2())
                item.isChecked = true
        }
        popupMenu.setOnDismissListener { leanbackOverlayFragment.setFading(true) }
        popupMenu.setOnMenuItemClickListener { item ->
            playbackController.playbackSpeed = item.title.toString().toFloat()
            popupMenu.dismiss()
            true
        }
        popupMenu.show()
    }

    init {
        initializeWithIcon(R.drawable.exo_ic_speed)
    }
}
