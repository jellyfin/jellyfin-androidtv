package org.jellyfin.androidtv.ui.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.*
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.databinding.VerticalGridBinding
import timber.log.Timber

class VerticalGridPresenter @JvmOverloads constructor(
	private val zoomFactor: Int = FocusHighlight.ZOOM_FACTOR_LARGE
) : Presenter() {

	// ------------------------------------------------------------------------
	// ViewHolder hierarchy (type-safe)
	// ------------------------------------------------------------------------

	sealed class GridHolder(view: View) : Presenter.ViewHolder(view)

	class ViewHolder(val gridView: VerticalGridView) :
		GridHolder(gridView) {

		val itemBridgeAdapter = ItemBridgeAdapter()
		var initialized: Boolean = false
	}

	// ------------------------------------------------------------------------
	// Configuration
	// ------------------------------------------------------------------------

	private var numRows: Int = -1
	private var numCols: Int = -1

	private var shadowEnabled: Boolean = true
	private var roundedCornersEnabled: Boolean = true

	private var onItemViewSelectedListener: OnItemViewSelectedListener? = null
	private var onItemViewClickedListener: OnItemViewClickedListener? = null

	// ------------------------------------------------------------------------
	// Public API
	// ------------------------------------------------------------------------

	fun setPosition(index: Int) {
		// optional convenience – only works after bind
	}

	fun setNumberOfRows(rows: Int) {
		require(rows >= 0) { "Invalid number of rows" }
		numRows = rows
	}

	fun setNumberOfColumns(cols: Int) {
		require(cols >= 0) { "Invalid number of columns" }
		numCols = cols
	}

	fun getNumberOfRows(): Int = numRows
	fun getNumberOfColumns(): Int = numCols

	fun setShadowEnabled(enabled: Boolean) {
		shadowEnabled = enabled
	}

	fun enableChildRoundedCorners(enable: Boolean) {
		roundedCornersEnabled = enable
	}

	fun setOnItemViewSelectedListener(listener: OnItemViewSelectedListener?) {
		onItemViewSelectedListener = listener
	}

	fun setOnItemViewClickedListener(listener: OnItemViewClickedListener?) {
		onItemViewClickedListener = listener
	}

	// ------------------------------------------------------------------------
	// Shadow / Z-order hooks
	// ------------------------------------------------------------------------

	open fun isUsingDefaultShadow(): Boolean =
		ShadowOverlayContainer.supportsShadow()

	open fun isUsingZOrder(): Boolean = false

	private fun needsDefaultShadow(): Boolean =
		shadowEnabled && isUsingDefaultShadow()

	// ------------------------------------------------------------------------
	// Presenter overrides
	// ------------------------------------------------------------------------

	override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
		val vh = createGridViewHolder(parent)
		initializeGridViewHolder(vh)
		check(vh.initialized) {
			"initializeGridViewHolder() must call super"
		}
		return vh
	}

	override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
		val vh = viewHolder.asGrid()

		Timber.d("onBindViewHolder %s", item)

		// Leanback may bind with null or non-ObjectAdapter items
		val adapter = item as? ObjectAdapter
		if (adapter == null) {
			vh.itemBridgeAdapter.setAdapter(null)
			vh.gridView.adapter = null
			return
		}

		vh.itemBridgeAdapter.setAdapter(adapter)
		vh.gridView.adapter = vh.itemBridgeAdapter
	}

	override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
		val vh = viewHolder.asGrid()

		Timber.d("onUnbindViewHolder")

		vh.itemBridgeAdapter.setAdapter(null)
		vh.gridView.adapter = null
	}

	// ------------------------------------------------------------------------
	// ViewHolder creation / initialization
	// ------------------------------------------------------------------------

	protected open fun createGridViewHolder(parent: ViewGroup): ViewHolder {
		val binding = VerticalGridBinding.inflate(
			LayoutInflater.from(parent.context),
			parent,
			false
		)
		return ViewHolder(binding.verticalGrid)
	}

	protected open fun initializeGridViewHolder(vh: ViewHolder) {
		require(numCols >= 0) { "Number of columns must be set" }

		Timber.d("Initializing grid: rows=%s cols=%s", numRows, numCols)

		vh.gridView.apply {
			setNumColumns(numCols)
			isFocusDrawingOrderEnabled = !isUsingZOrder()

			setOnChildViewHolderSelectedListener(
				object : OnChildViewHolderSelectedListener() {
					override fun onChildViewHolderSelected(
						parent: RecyclerView,
						child: RecyclerView.ViewHolder?,
						position: Int,
						subposition: Int
					) {
						selectChildView(vh, child?.itemView)
					}
				}
			)
		}

		vh.itemBridgeAdapter.apply {
			setWrapper(wrapper)
			setAdapterListener(adapterListener)
		}

		if (needsDefaultShadow() || roundedCornersEnabled) {
			ShadowOverlayContainer.prepareParentForShadow(vh.gridView)
			(vh.view as ViewGroup).clipChildren = false
		}

		FocusHighlightHelper.setupBrowseItemFocusHighlight(
			vh.itemBridgeAdapter,
			zoomFactor,
			true
		)

		vh.initialized = true
	}

	// ------------------------------------------------------------------------
	// Adapter helpers
	// ------------------------------------------------------------------------

	private val wrapper = object : ItemBridgeAdapter.Wrapper() {
		override fun createWrapper(root: View): View =
			ShadowOverlayContainer(root.context).apply {
				layoutParams = ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				)
				initialize(
					needsDefaultShadow(),
					true,
					roundedCornersEnabled
				)
			}

		override fun wrap(wrapper: View, wrapped: View) {
			(wrapper as ShadowOverlayContainer).wrap(wrapped)
		}
	}

	private val adapterListener = object : ItemBridgeAdapter.AdapterListener() {

		override fun onBind(holder: ItemBridgeAdapter.ViewHolder) {
			val listener = onItemViewClickedListener ?: return
			holder.viewHolder.view.setOnClickListener {
				listener.onItemClicked(
					holder.viewHolder,
					holder.item,
					null,
					null
				)
			}
		}

		override fun onUnbind(holder: ItemBridgeAdapter.ViewHolder) {
			holder.viewHolder.view.setOnClickListener(null)
		}

		override fun onAttachedToWindow(holder: ItemBridgeAdapter.ViewHolder) {
			holder.itemView.isActivated = true
		}
	}

	// ------------------------------------------------------------------------
	// Selection handling
	// ------------------------------------------------------------------------

	private fun selectChildView(vh: ViewHolder, view: View?) {
		val listener = onItemViewSelectedListener ?: return

		val ibh = view?.let {
			vh.gridView.getChildViewHolder(it) as? ItemBridgeAdapter.ViewHolder
		}

		if (ibh == null) {
			listener.onItemSelected(null, null, null, null)
		} else {
			listener.onItemSelected(
				ibh.viewHolder,
				ibh.item,
				null,
				null
			)
		}
	}

	// ------------------------------------------------------------------------
	// Type-safe helper
	// ------------------------------------------------------------------------

	private fun Presenter.ViewHolder.asGrid(): ViewHolder {
		require(this is ViewHolder) {
			"Unexpected ViewHolder type: ${this::class.java.name}"
		}
		return this
	}
}
