package org.jellyfin.androidtv.data.compat

import org.jellyfin.apiclient.model.dlna.SubtitleDeliveryMethod

class SubtitleStreamInfo {
	var url: String? = null
	var name: String? = null
	var format: String? = null
	var displayTitle: String? = null
	var index = 0
	var deliveryMethod = SubtitleDeliveryMethod.Encode
}
