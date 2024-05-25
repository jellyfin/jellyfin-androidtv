package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

enum class PlayerMenuBackgroundColor(
        override val nameRes: Int,
) : PreferenceEnum {
    Transparent(R.string.pref_player_menu_background_transparent),
    Light(R.string.pref_player_menu_background_light),
    Dark(R.string.pref_player_menu_background_dark),
}
