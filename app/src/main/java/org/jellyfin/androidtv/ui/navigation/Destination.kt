package org.jellyfin.androidtv.ui.navigation

import android.app.Activity
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

sealed interface Destination {
	data class Fragment(
		val fragment: KClass<out androidx.fragment.app.Fragment>,
		val arguments: Bundle = bundleOf(),
	) : Destination

	data class Activity(
		val activity: KClass<out android.app.Activity>,
		val extras: Bundle = bundleOf(),
	) : Destination
}

inline fun <reified T : Fragment> fragmentDestination(
	vararg arguments: Pair<String, Any?>,
) = Destination.Fragment(
	fragment = T::class,
	arguments = bundleOf(*arguments),
)

inline fun <reified T : Activity> activityDestination(
	vararg extras: Pair<String, Any?>,
) = Destination.Activity(
	activity = T::class,
	extras = bundleOf(*extras),
)
