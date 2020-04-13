package org.jellyfin.androidtv.util

import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.TvApp
import org.jellyfin.apiclient.interaction.ApiClient

val Fragment.apiClient: ApiClient by lazy { TvApp.getApplication().apiClient }
