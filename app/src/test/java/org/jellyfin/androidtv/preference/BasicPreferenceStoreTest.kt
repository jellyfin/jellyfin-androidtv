package org.jellyfin.androidtv.preference

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PreferenceStoreTest {
	class TestStub : PreferenceStore() {
		var key: String? = null
		var int: Int? = null
		var long: Long? = null
		var bool: Boolean? = null
		var string: String? = null

		override fun getInt(key: String, defaultValue: Int): Int {
			return int ?: 0
		}

		override fun getLong(key: String, defaultValue: Long): Long {
			return long ?: 0
		}

		override fun getBool(key: String, defaultValue: Boolean): Boolean {
			return bool ?: false
		}

		override fun getString(key: String, defaultValue: String): String {
			return string ?: ""
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
		// Check it was serialised correctly
		assertEquals(expectedVal.toString(), instance.string)
	}

}
