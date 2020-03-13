package org.jellyfin.androidtv.details

import androidx.leanback.widget.Row
import org.jellyfin.androidtv.details.actions.Action
import org.jellyfin.androidtv.model.itemtypes.BaseItem

class DetailsOverviewRow(val item: BaseItem, val actions: List<Action>) : Row()
