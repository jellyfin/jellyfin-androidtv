package org.jellyfin.androidtv.data.querying

import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.apiclient.model.querying.ItemFields
import org.jellyfin.apiclient.model.querying.ItemQuery
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class StdItemQuery @JvmOverloads constructor(
	fields: Array<ItemFields> = defaultFields
) : ItemQuery(), KoinComponent {
	companion object {
		val defaultFields = arrayOf(
			ItemFields.PrimaryImageAspectRatio,
			ItemFields.Overview,
			ItemFields.ItemCounts,
			ItemFields.DisplayPreferencesId,
			ItemFields.ChildCount
		)
	}

	init {
		userId = get<UserRepository>().currentUser.value!!.id.toString()
		setFields(fields)
	}
}
