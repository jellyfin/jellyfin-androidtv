package org.jellyfin.androidtv.preference

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BasicPreferenceStoreTest {
	class TestStub : BasicPreferenceStore() {
		var keyName: String? = null
		var int: Int? = null
		var long: Long? = null
		var bool: Boolean? = null
		var string: String? = null

		override fun getInt(keyName: String, defaultValue: Int): Int {
			return int ?: 0
		}

		override fun getLong(keyName: String, defaultValue: Long): Long {
			return long ?: 0
		}

		override fun getBool(keyName: String, defaultValue: Boolean): Boolean {
			return bool ?: false
		}

		override fun getString(keyName: String, defaultValue: String): String {
			return string ?: ""
		}

		override fun setInt(keyName: String, value: Int) {
			this.keyName = keyName
			int = value
		}

		override fun setLong(keyName: String, value: Long) {
			this.keyName = keyName
			long = value
		}

		override fun setBool(keyName: String, value: Boolean) {
			this.keyName = keyName
			bool = value
		}

		override fun setString(keyName: String, value: String) {
			this.keyName = keyName
			string = value
		}

		override fun <T : Preference<V>, V : Any> delete(preference: T) {
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
		val pref = Preference.int("keyName", 0)
		val expectedVal = 1
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.keyName)
	}

	@Test
	fun testGetSetLong() {
		val pref = Preference.long("keyName", 0)
		val expectedVal: Long = 1
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.keyName)
	}

	@Test
	fun testGetSetBoolean() {
		val pref = Preference.boolean("keyName", false)
		val expectedVal = true
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.keyName)
	}

	@Test
	fun testGetSetString() {
		val pref = Preference.string("keyName", "")
		val expectedVal = "val"
		instance[pref] = expectedVal
		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.keyName)
	}

	enum class TestEnum { NOT_SET, SET }

	@Test
	fun testGetSetEnum() {
		val pref = Preference.enum("keyName", TestEnum.NOT_SET)
		val expectedVal = TestEnum.SET

		instance[pref] = expectedVal

		assertEquals(expectedVal, instance[pref])
		assertEquals(pref.key, instance.keyName)
		// Check it was serialised correctly
		assertEquals(expectedVal.toString(), instance.string)
	}

}
