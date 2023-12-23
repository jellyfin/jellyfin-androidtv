package org.jellyfin.androidtv.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.leanback.app.RowsSupportFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.databinding.FragmentSearchTextBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TextSearchFragment : Fragment() {
	private val viewModel: SearchViewModel by viewModel()

	private var _binding: FragmentSearchTextBinding? = null
	private val binding get() = _binding!!

	private val searchFragmentDelegate: SearchFragmentDelegate by inject {
		parametersOf(requireContext())
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = FragmentSearchTextBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		binding.searchBar.apply {
			onTextChanged { viewModel.searchDebounced(it) }
			onSubmit { viewModel.searchImmediately(it) }
		}

		binding.resultsFrame.getFragment<RowsSupportFragment?>()?.let {
			it.adapter = searchFragmentDelegate.rowsAdapter
			it.onItemViewClickedListener = searchFragmentDelegate.onItemViewClickedListener
			it.onItemViewSelectedListener = searchFragmentDelegate.onItemViewSelectedListener
		}

		viewModel.searchResultsFlow
			.onEach { searchFragmentDelegate.showResults(it) }
			.launchIn(lifecycleScope)
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	private fun EditText.onSubmit(onSubmit: (String) -> Unit) {
		setOnEditorActionListener { view, actionId, _ ->
			when (actionId) {
				EditorInfo.IME_ACTION_DONE,
				EditorInfo.IME_ACTION_SEARCH,
				EditorInfo.IME_ACTION_PREVIOUS -> {
					onSubmit(text.toString())

					// Manually close IME to workaround focus issue with Fire TV
					context.getSystemService<InputMethodManager>()
						?.hideSoftInputFromWindow(view.windowToken, 0)

					// Focus on search results
					binding.resultsFrame.requestFocus()
					true
				}

				else -> false
			}
		}
	}

	private fun EditText.onTextChanged(onTextChanged: (String) -> Unit) {
		addTextChangedListener(object : TextWatcher {
			override fun afterTextChanged(
				s: Editable,
			) = onTextChanged(s.toString())

			override fun beforeTextChanged(
				s: CharSequence,
				start: Int,
				count: Int,
				after: Int,
			) = Unit

			override fun onTextChanged(
				s: CharSequence,
				start: Int,
				before: Int,
				count: Int,
			) = Unit
		})
	}
}
