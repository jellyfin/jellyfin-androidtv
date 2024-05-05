package org.jellyfin.playback.core.element

/**
 * A key to identify the type of an element.
 */
class ElementKey<T : Any>(val name: String) {
	override fun toString(): String = "ElementKey $name"
}
