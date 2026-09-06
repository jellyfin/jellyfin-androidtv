package org.jellyfin.androidtv.ui.navigation.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.InspectorInfo

/**
 * Add a unique identifier to this composable to automatically restore focus. Most commonly when navigating back a page within a router or
 * on activity recreation. The focus keys automatically use their parent keys as suffix. When no focus is being restored, [initialFocus]
 * can be used to focus this composable the first time it is placed.
 */
@Composable
fun Modifier.focusKey(
	key: String,
	focusRequester: FocusRequester = remember { FocusRequester() },
	initialFocus: Boolean = false,
): Modifier = this
	.focusRequester(focusRequester)
	.then(FocusKeyElement(key, focusRequester, initialFocus))

private data class FocusKeyElement(
	val key: String,
	val focusRequester: FocusRequester,
	val initialFocus: Boolean,
) : ModifierNodeElement<FocusKeyNode>() {
	override fun create() = FocusKeyNode(key, focusRequester, initialFocus)

	override fun update(node: FocusKeyNode) {
		node.update(key, focusRequester, initialFocus)
	}

	override fun InspectorInfo.inspectableProperties() {
		name = "focusKey"
		properties["key"] = key
		properties["focusRequester"] = focusRequester
		properties["initialFocus"] = initialFocus
	}
}

private class FocusKeyNode(
	var key: String,
	var focusRequester: FocusRequester,
	var initialFocus: Boolean,
) : Modifier.Node(),
	TraversableNode,
	FocusEventModifierNode,
	CompositionLocalConsumerModifierNode,
	LayoutAwareModifierNode {

	override val traverseKey: Any = FocusKeyNode::class

	private var cachedKey: String? = null

	private val fullKey: String
		get() {
			val parent = findNearestAncestor(traverseKey) as? FocusKeyNode
			return parent?.let { "${it.fullKey}.$key" } ?: key
		}

	private val manager
		get() = currentValueOf(LocalFocusPersistManager)

	override fun onAttach() {
		updateRegistration()
	}

	fun update(key: String, focusRequester: FocusRequester, initialFocus: Boolean) {
		val oldKey = fullKey

		this.key = key

		val keyChanged = oldKey != fullKey
		val requesterChanged = this.focusRequester !== focusRequester
		val initialFocusChanged = this.initialFocus != initialFocus

		if (requesterChanged) {
			this.focusRequester = focusRequester
		}
		this.initialFocus = initialFocus

		if (requesterChanged || keyChanged || initialFocusChanged) {
			updateRegistration()
		}

		if (keyChanged) {
			notifyDescendants()
		}
	}

	fun onParentChanged() {
		updateRegistration()
		notifyDescendants()
	}

	private fun notifyDescendants() {
		traverseDescendants(traverseKey) { node ->
			when (node) {
				is FocusKeyNode -> {
					node.onParentChanged()
					TraverseDescendantsAction.SkipSubtreeAndContinueTraversal
				}

				else ->
					TraverseDescendantsAction.ContinueTraversal
			}
		}
	}

	private fun updateRegistration() {
		val newFullKey = fullKey
		if (newFullKey == cachedKey) {
			manager.register(newFullKey, focusRequester, initialFocus)
			return
		}

		cachedKey?.let(manager::unregister)
		cachedKey = newFullKey
		manager.register(newFullKey, focusRequester, initialFocus)
	}

	override fun onDetach() {
		cachedKey?.let { manager.unregister(it) }
		cachedKey = null
	}

	override fun onFocusEvent(focusState: FocusState) {
		if (focusState.isFocused) manager.onFocusChanged(fullKey, true)
	}

	override fun onPlaced(coordinates: LayoutCoordinates) {
		if (initialFocus) manager.requestInitialFocus(focusRequester)
	}
}
