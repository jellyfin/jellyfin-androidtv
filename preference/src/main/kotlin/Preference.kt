package org.jellyfin.preference

import kotlin.reflect.KClass

data class Preference<T : Any>(
	val key: String,
	val defaultValue: T,
	val type: KClass<T>
) {
	companion object {
		fun int(key: String, defaultValue: Int) = Preference(key, defaultValue, Int::class)
		fun long(key: String, defaultValue: Long) = Preference(key, defaultValue, Long::class)
		fun boolean(key: String, defaultValue: Boolean) = Preference(key, defaultValue, Boolean::class)
		fun string(key: String, defaultValue: String) = Preference(key, defaultValue, String::class)
		inline fun <reified T : Any> enum(key: String, defaultValue: T) = Preference(key, defaultValue, T::class)
	}
}
