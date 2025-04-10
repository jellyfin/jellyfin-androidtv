package org.jellyfin.androidtv.di

import org.jellyfin.androidtv.ui.home.HomeFragmentLiveTVRowWithUserPreferenceCheck
import org.koin.dsl.module

val liveTvModule = module {
    factory { (activity: android.app.Activity) ->
        HomeFragmentLiveTVRowWithUserPreferenceCheck(
            activity = activity,
            userRepository = get(),
            navigationRepository = get(),
            api = get(),
            coroutineScope = get()
        )
    }
}
