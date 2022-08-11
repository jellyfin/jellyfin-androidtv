package org.jellyfin.androidtv.util

import android.view.KeyEvent
import timber.log.Timber

@DslMarker
annotation class KeyHandlerDsl

class KeyHandler(
	private val actions: Collection<KeyHandlerAction>
) {
	fun onKey(event: KeyEvent?): Boolean {
		if (event == null) return false

		val action = actions
			.filter { it.keys.contains(event.keyCode) }
			.filter { it.type == event.action }
			.filter { it.conditions.isEmpty() || it.conditions.all { predicate -> predicate.invoke() } }
			.firstOrNull() ?: return false

		Timber.d("Key press detected: code=${event.keyCode} type=${event.action} action=$action")

		action.body()
		return true
	}
}

data class KeyHandlerAction(
	val type: Int,
	val keys: Collection<Int>,
	val conditions: Collection<() -> Boolean>,
	val body: () -> Unit
)

class KeyHandlerBuilder {
	private val actions = mutableListOf<KeyHandlerAction>()

	fun addAction(action: KeyHandlerAction) {
		actions.add(action)
	}

	fun build(): KeyHandler = KeyHandler(actions)

	@KeyHandlerDsl
	fun keyDown(vararg keys: Int) = KeyHandlerActionBuilder(this).apply {
		setType(KeyEvent.ACTION_DOWN)
		for (key in keys) addKey(key)
	}

	@KeyHandlerDsl
	fun keyDown(vararg keys: Int, action: () -> Unit) = KeyHandlerActionBuilder(this).apply {
		setType(KeyEvent.ACTION_DOWN)
		for (key in keys) addKey(key)
		setBody(action)
	}.build()

	@KeyHandlerDsl
	fun keyUp(vararg keys: Int) = KeyHandlerActionBuilder(this).apply {
		setType(KeyEvent.ACTION_UP)
		for (key in keys) addKey(key)
	}

	@KeyHandlerDsl
	fun keyUp(vararg keys: Int, action: () -> Unit) = KeyHandlerActionBuilder(this).apply {
		setType(KeyEvent.ACTION_UP)
		for (key in keys) addKey(key)
		setBody(action)
	}.build()
}

class KeyHandlerActionBuilder(
	private val context: KeyHandlerBuilder
) {
	private var type = KeyEvent.ACTION_DOWN
	private val keys = mutableListOf<Int>()
	private val conditions = mutableListOf<() -> Boolean>()
	private var body: (() -> Unit)? = null

	fun setType(type: Int) {
		require(type == KeyEvent.ACTION_DOWN || type == KeyEvent.ACTION_UP) {
			"Type must be KeyEvent.ACTION_DOWN or KeyEvent.ACTION_UP"
		}

		this.type = type
	}

	fun addKey(key: Int) {
		keys.add(key)
	}

	fun addCondition(condition: () -> Boolean) {
		conditions.add(condition)
	}

	fun setBody(body: () -> Unit) {
		this.body = body
	}

	fun build() {
		require(keys.isNotEmpty()) { "Keys should contain at least 1 key" }
		requireNotNull(body) { "Body must be set" }

		val action = KeyHandlerAction(type, keys, conditions, body!!)
		context.addAction(action)
	}

	@KeyHandlerDsl
	fun condition(condition: () -> Boolean) = apply {
		addCondition(condition)
	}

	@KeyHandlerDsl
	fun condition(condition: () -> Boolean, action: () -> Unit) = apply {
		addCondition(condition)
		setBody(action)
	}.build()

	@KeyHandlerDsl
	fun body(action: () -> Unit) = apply {
		setBody(action)
	}.build()
}

@KeyHandlerDsl
inline fun createKeyHandler(body: KeyHandlerBuilder.() -> Unit) = KeyHandlerBuilder().apply {
	body()
}.build()
