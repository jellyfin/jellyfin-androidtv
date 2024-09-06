package org.jellyfin.androidtv.integration.dream

import android.service.dreams.DreamService
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

abstract class DreamServiceCompat : DreamService(), SavedStateRegistryOwner, ViewModelStoreOwner {
	@Suppress("LeakingThis")
	private val lifecycleRegistry = LifecycleRegistry(this)

	@Suppress("LeakingThis")
	private val savedStateRegistryController = SavedStateRegistryController.create(this).apply {
		performAttach()
	}

	override val lifecycle: Lifecycle get() = lifecycleRegistry
	override val viewModelStore = ViewModelStore()
	override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

	@CallSuper
	override fun onCreate() {
		super.onCreate()

		savedStateRegistryController.performRestore(null)
		lifecycleRegistry.currentState = Lifecycle.State.CREATED
	}

	override fun onDreamingStarted() {
		super.onDreamingStarted()

		lifecycleRegistry.currentState = Lifecycle.State.STARTED
	}

	override fun onDreamingStopped() {
		super.onDreamingStopped()

		lifecycleRegistry.currentState = Lifecycle.State.CREATED
	}

	fun setContent(content: @Composable () -> Unit) {
		val view = ComposeView(this)
		// Set composition strategy
		view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

		// Inject dependencies normally added by appcompat activities
		view.setViewTreeLifecycleOwner(this)
		view.setViewTreeViewModelStoreOwner(this)
		view.setViewTreeSavedStateRegistryOwner(this)

		// Set content composable
		view.setContent(content)

		// Set content view
		setContentView(view)
	}
}
