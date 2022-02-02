package org.jellyfin.androidtv.preference

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests that the generic set/get methods dispatch to the correct
 * internal getT/setT methods
 */
class PreferenceStoreTest {
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

	@Before
	fun setup() {
		instance = TestStub()
	}

	private lateinit var instance: TestStub

	@Test
	fun testGetSetInt() {
		val pref = Preference.int("key", 0)
		val expectedVal = 1
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.key)
	}

	@Test
	fun testGetSetLong() {
		val pref = Preference.long("key", 0)
		val expectedVal: Long = 1
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.key)
	}

	@Test
	fun testGetSetBoolean() {
		val pref = Preference.boolean("key", false)
		val expectedVal = true
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.key)
	}

	@Test
	fun testGetSetString() {
		val pref = Preference.string("key", "")
		val expectedVal = "val"
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.key)
	}

	enum class TestEnum { NOT_SET, SET }

	@Test
	fun testGetSetEnum() {
		val pref = Preference.enum("key", TestEnum.NOT_SET)
		val expectedVal = TestEnum.SET
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.key)
	}

}
