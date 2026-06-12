package org.jellyfin.androidtv.ui.playback.theme

import androidx.fragment.app.Fragment
import org.koin.androidx.viewmodel.ext.android.activityViewModel

fun getThemeAudioViewModel(fragment: Fragment): ThemeAudioViewModel {
	val lazyViewModel: ThemeAudioViewModel by fragment.activityViewModel()
	return lazyViewModel
}
