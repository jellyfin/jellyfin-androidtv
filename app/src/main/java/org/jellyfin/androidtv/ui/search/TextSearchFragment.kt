package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.BaseOnItemViewClickedListener
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewSelectedListener
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentSearchTextBinding
import org.jellyfin.androidtv.ui.itemhandling.BaseRowItem
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.koin.java.KoinJavaComponent.inject

class TextSearchFragment : Fragment(), TextWatcher, TextView.OnEditorActionListener {
	private lateinit var searchProvider: SearchProvider
	private val backgroundService = inject<BackgroundService>(BackgroundService::class.java)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Create provider
		searchProvider = SearchProvider(requireContext(), lifecycle)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
		FragmentSearchTextBinding.inflate(inflater, container, false).root

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		// Add event listeners
		requireActivity().findViewById<EditText>(R.id.search_bar).apply {
			addTextChangedListener(this@TextSearchFragment)
			setOnEditorActionListener(this@TextSearchFragment)
		}

		// Set up result fragment
		val rowsSupportFragment = childFragmentManager.findFragmentById(R.id.results_frame) as? RowsSupportFragment
		rowsSupportFragment?.adapter = searchProvider.resultsAdapter

		rowsSupportFragment?.onItemViewClickedListener = BaseOnItemViewClickedListener<ListRow> { _, item, _, row ->
			if (item !is BaseRowItem) return@BaseOnItemViewClickedListener

			val adapter = row.adapter as ItemRowAdapter
			ItemLauncher.launch(item as BaseRowItem?, adapter, item.index, activity)
		}

		rowsSupportFragment?.onItemViewSelectedListener = OnItemViewSelectedListener { _, item, _, _ ->
			if (item is BaseRowItem) backgroundService.value.setBackground(item.searchHint!!)
			else backgroundService.value.clearBackgrounds()
		}
	}

	override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
	override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

	override fun afterTextChanged(editable: Editable) {
		searchProvider.onQueryTextChange(editable.toString())
	}

	override fun onEditorAction(view: TextView, actionId: Int, event: KeyEvent?): Boolean {
		// Detect keyboard "submit" actions
		if (actionId == EditorInfo.IME_ACTION_SEARCH) searchProvider.onQueryTextSubmit(view.text.toString())

		// Return "false" to automatically close keyboard
		return false
	}
}
