package org.jellyfin.androidtv.ui.settings.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jellyfin.preference.Preference
import org.jellyfin.preference.store.AsyncPreferenceStore
import org.jellyfin.preference.store.PreferenceStore

/**
 * Utility to wrap the [PreferenceStore] getter/setter as [MutableState] to recompose whne the value is changed. Uses a **local** state for
 * the preference, updates to the preference not done via this function won't trigger recomposition or update its value.
 */
@Composable
fun <ME, MV, T : Any> rememberPreference(store: PreferenceStore<ME, MV>, preference: Preference<T>): MutableState<T> {
	val mutableState = remember { mutableStateOf(store[preference]) }
	LaunchedEffect(mutableState.value) {
		if (store[preference] != mutableState.value) {
			store[preference] = mutableState.value
			if (store is AsyncPreferenceStore) store.commit()
		}
	}
	return mutableState
}

@Composable
@JvmName("rememberEnumPreference")
fun <ME, MV, T : Enum<T>> rememberPreference(
	store: PreferenceStore<ME, MV>,
	preference: Preference<T>
): MutableState<T> {
	val mutableState = remember { mutableStateOf(store[preference]) }
	LaunchedEffect(mutableState.value) {
		if (store[preference] != mutableState.value) {
			store[preference] = mutableState.value
			if (store is AsyncPreferenceStore) store.commit()
		}
	}
	return mutableState
}
