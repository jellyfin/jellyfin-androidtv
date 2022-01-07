package org.jellyfin.androidtv.preference

class UnsupportedPreferenceType : Exception {
	constructor(message: String) : super(message)
	constructor(message: String, cause: Throwable) : super(message, cause)
	constructor(cause: Throwable) : super(cause)
}

sealed class PreferenceVal<T>(initialValue: T) {
	var data: T = initialValue

	class BoolT(data: Boolean) : PreferenceVal<Boolean>(data)
	class IntT(data: Int) : PreferenceVal<Int>(data)
	class LongT(data: Long) : PreferenceVal<Long>(data)
	class StringT(data: String) : PreferenceVal<String>(data)
	class EnumT<T : Enum<T>>(data: T) : PreferenceVal<T>(data) {
		val enumClass = data::class
	}

	companion object {
		@Suppress("UNCHECKED_CAST")
		fun <T : Any> buildBasedOnT(value: T): PreferenceVal<T> =
			when (value) {
				is Boolean -> BoolT(value)
				is Int -> IntT(value)
				is Long -> LongT(value)
				is String -> StringT(value)
				else -> throw UnsupportedPreferenceType("Unknown Type")
			} as PreferenceVal<T>

		fun <T : Enum<T>> buildBasedOnT(value: T): PreferenceVal<T> = EnumT(value)
	}
}

data class Preference<T>(
	val key: String,
	val defaultValue: PreferenceVal<T>
	// The currentValue is managed by Android's Pref Store
) {
	companion object {
		fun int(key: String, defaultValue: Int) =
			Preference(key, PreferenceVal.IntT(defaultValue))

		fun long(key: String, defaultValue: Long) =
			Preference(key, PreferenceVal.LongT(defaultValue))

		fun boolean(key: String, defaultValue: Boolean) =
			Preference(key, PreferenceVal.BoolT(defaultValue))

		fun string(key: String, defaultValue: String) =
			Preference(key, PreferenceVal.StringT(defaultValue))

		inline fun <reified T : Enum<T>> enum(key: String, defaultValue: T) =
			Preference(key, PreferenceVal.EnumT(defaultValue))
	}
}
