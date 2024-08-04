package org.jellyfin.preference

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.preference.migration.MigrationContext
import org.jellyfin.preference.store.PreferenceStore

/**
 * Tests that the generic set/get methods dispatch to the correct
 * internal getT/setT methods
 */
class PreferenceStoreTests : FunSpec({
	fun <T : Any> verifySimpleType(expectedVal: T, preference: Preference<T>) {
		val instance = TestStub()
		instance[preference] = expectedVal
		instance[preference] shouldBe expectedVal
		instance.key shouldBe preference.key
	}

	test("Reading and writing primitives works correctly") {
		verifySimpleType(1, intPreference("key", 0))
		verifySimpleType(1L, longPreference("key", 0L))
		verifySimpleType(1f, floatPreference("key", 0f))
		verifySimpleType(true, booleanPreference("key", false))
		verifySimpleType("string", stringPreference("key", ""))
	}

	test("Reading and writing enums works correctly") {
		val pref = enumPreference("key", TestEnum.NOT_SET)
		val expectedVal = TestEnum.SET
		val instance = TestStub()
		instance[pref] = expectedVal
		instance[pref] shouldBe expectedVal
		pref.key shouldBe instance.key
	}
})

private class TestStub : PreferenceStore<Unit, Unit>() {
	var key: String? = null
	private var int: Int? = null
	private var long: Long? = null
	private var float: Float? = null
	private var bool: Boolean? = null
	private var string: String? = null
	private var enum: Enum<*>? = null

	@Suppress("UNCHECKED_CAST")
	override fun <T : Enum<T>> getEnum(preference: Preference<T>): T =
		(this.enum ?: preference.defaultValue) as T

	override fun getInt(key: String, defaultValue: Int): Int = int ?: 0
	override fun getLong(key: String, defaultValue: Long): Long = long ?: 0L
	override fun getFloat(key: String, defaultValue: Float): Float = float ?: 0f
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

	override fun setFloat(key: String, value: Float) {
		this.key = key
		float = value
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

	override fun runMigrations(body: MigrationContext<Unit, Unit>.() -> Unit) {
		throw NotImplementedError("Not required for tests")
	}
}

private enum class TestEnum { NOT_SET, SET }
