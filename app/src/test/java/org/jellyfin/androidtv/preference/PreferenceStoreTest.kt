package org.jellyfin.androidtv.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe

/**
 * Tests that the generic set/get methods dispatch to the correct
 * internal getT/setT methods
 */
class PreferenceStoreTest : FunSpec() {
	class TestStub : PreferenceStore() {
		var key: String? = null
		private var int: Int? = null
		private var long: Long? = null
		private var bool: Boolean? = null
		private var string: String? = null
		private var enum: Enum<*>? = null

		@Suppress("UNCHECKED_CAST")
		override fun <T : Enum<T>> getEnum(preference: Preference<T>): T =
			(this.enum ?: preference.defaultValue) as T

		override fun getInt(key: String, defaultValue: Int): Int = int ?: 0
		override fun getLong(key: String, defaultValue: Long): Long = long ?: 0
		override fun getBool(key: String, defaultValue: Boolean): Boolean = bool ?: false
		override fun getString(key: String, defaultValue: String) = string ?: ""

		override fun <V : Enum<V>> setEnum(preference: Preference<*>, value: Enum<V>) {
			key = preference.key
			enum = value
		}

		override fun setInt(key: String, value: Int) {
			this.key = key
			int = value
		}

		override fun setLong(key: String, value: Long) {
			this.key = key
			long = value
		}

		override fun setBool(key: String, value: Boolean) {
			this.key = key
			bool = value
		}

		override fun setString(key: String, value: String) {
			this.key = key
			string = value
		}


		override fun <T : Any> delete(preference: Preference<T>) {
			throw NotImplementedError("Not required for tests")
		}
	}

	var instance = TestStub()
	override fun beforeTest(f: suspend (TestCase) -> Unit) {
		super.beforeTest(f)
		instance = TestStub()
	}

	enum class TestEnum { NOT_SET, SET }

	fun <T : Any> verifySimpleType(expectedVal: T, preference: Preference<T>) {
		instance[preference] = expectedVal
		instance[preference] shouldBe expectedVal
		instance.key shouldBe preference.key
	}

	init {
		test("get and setting native types") {
			// Unfortunately KoTest will try to serialise to a base Serializable using rows
			// So manually use generics for the same effect
			verifySimpleType(1, Preference.int("key", 0))
			verifySimpleType(1L, Preference.long("key", 0L))
			verifySimpleType(true, Preference.boolean("key", false))
			verifySimpleType("string", Preference.string("key", ""))
		}

		test("get and set with enum types") {
			// Generics removes too much type info for us to use verifySimpleType
			// we could be clever, but I prefer duplication and to KISS
			val pref = Preference.enum("key", TestEnum.NOT_SET)
			val expectedVal = TestEnum.SET
			instance[pref] = expectedVal
			instance[pref] shouldBe expectedVal
			pref.key shouldBe instance.key
		}

	}

}
