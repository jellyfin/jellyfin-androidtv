package org.jellyfin.androidtv.data.querying

import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.apiclient.model.querying.ItemFields
import org.jellyfin.apiclient.model.querying.ItemQuery
import org.koin.java.KoinJavaComponent.get

class StdItemQuery @JvmOverloads constructor(
	fields: Array<ItemFields> = defaultFields
) : ItemQuery() {
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
		userId = get<UserRepository>(UserRepository::class.java).currentUser.value!!.id.toString()
		setFields(fields)
	}
}
