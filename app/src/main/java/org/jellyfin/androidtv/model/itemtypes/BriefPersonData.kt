package org.jellyfin.androidtv.model.itemtypes

import org.jellyfin.apiclient.model.dto.BaseItemPerson
import org.jellyfin.apiclient.model.entities.ImageType
import org.jellyfin.apiclient.model.entities.PersonType

class BriefPersonData(original: BaseItemPerson) {
	val id: String = original.id
	val name: String = original.name
	val role: String? = original.role
	val type: PersonType = original.personType
	val primaryImage: ImageCollection.Image? = original.primaryImageTag?.let { ImageCollection.Image(original.id, ImageType.Primary, it) }
}
