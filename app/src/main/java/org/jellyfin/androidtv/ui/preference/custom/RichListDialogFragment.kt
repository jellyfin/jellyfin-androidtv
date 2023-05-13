package org.jellyfin.androidtv.ui.preference.custom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.leanback.preference.LeanbackPreferenceDialogFragmentCompat
import androidx.leanback.widget.VerticalGridView
import androidx.preference.ListPreference
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.PreferenceRichListBinding

class RichListDialogFragment : LeanbackPreferenceDialogFragmentCompat() {
	companion object {
		fun newInstance(key: String) = RichListDialogFragment().apply {
			arguments = Bundle().apply {
				putString(ARG_KEY, key)
			}
		}
	}

	private var _binding: PreferenceRichListBinding? = null
	private val binding get() = _binding!!
	private lateinit var adapter: RecyclerView.Adapter<*>

	private fun <K> RichListPreference<K>.createAdapter() = Adapter(
		items = getItems(),
		selectedValue = value
	)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		adapter = when (val preference = preference) {
			// Adapt ListPreference to RichListItems
			is ListPreference -> Adapter(
				items = preference.entryValues.mapIndexed { index, key ->
					RichListItem.RichListOption(key.toString(), preference.entries[index].toString())
				},
				selectedValue = preference.value
			)

			is RichListPreference<*> -> preference.createAdapter()
			else -> throw NotImplementedError()
		}
	}

	public override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val styledContext = ContextThemeWrapper(activity, androidx.leanback.preference.R.style.PreferenceThemeOverlayLeanback)
		val styledInflater = inflater.cloneInContext(styledContext)
		_binding = PreferenceRichListBinding.inflate(styledInflater, container, false)

		// Dialog
		binding.decorTitle.text = preference.dialogTitle

		if (preference.dialogMessage?.isNotBlank() == true) {
			binding.message.text = preference.dialogMessage
			binding.message.isVisible = true
		}

		// Items
		val verticalGridView = binding.list
		verticalGridView.windowAlignment = VerticalGridView.WINDOW_ALIGN_BOTH_EDGE
		verticalGridView.adapter = adapter
		verticalGridView.requestFocus()

		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	/**
	 * Items used in [Adapter].
	 */
	sealed class RichListItem<K> {
		data class RichListSection<K>(
			val title: String
		) : RichListItem<K>()

		data class RichListOption<K>(
			val key: K,
			val title: String,
			val summary: String? = null
		) : RichListItem<K>()
	}

	inner class Adapter<K>(
		private val items: List<RichListItem<K>>,
		private var selectedValue: K? = null
	) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
		override fun getItemCount() = items.size

		override fun getItemViewType(position: Int) = when (items[position]) {
			is RichListItem.RichListOption<*> -> R.layout.preference_rich_list_option
			is RichListItem.RichListSection -> R.layout.preference_rich_list_category
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val inflater = LayoutInflater.from(parent.context)
			val view = inflater.inflate(viewType, parent, false)

			return when (viewType) {
				R.layout.preference_rich_list_option -> OptionViewHolder(view, ::onItemClick)
				R.layout.preference_rich_list_category -> CategoryViewHolder(view)
				else -> throw NotImplementedError()
			}
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = when (val item = items[position]) {
			is RichListItem.RichListOption<*> -> {
				holder as OptionViewHolder

				holder.button.isChecked = item.key == selectedValue
				holder.title.text = item.title
				holder.summary.text = item.summary
				holder.summary.isVisible = item.summary?.isNotBlank() == true
			}

			is RichListItem.RichListSection -> {
				holder as CategoryViewHolder

				holder.title.text = item.title
			}
		}

		fun onItemClick(viewHolder: OptionViewHolder) {
			val index = viewHolder.absoluteAdapterPosition
			if (index == RecyclerView.NO_POSITION) return

			val item = items[index] as RichListItem.RichListOption<K>
			if (preference.callChangeListener(item.key)) {
				when (val preference = preference) {
					is ListPreference -> preference.value = item.key as String
				}

				selectedValue = item.key
			}

			parentFragmentManager.popBackStack()
			notifyDataSetChanged()
		}
	}

	class OptionViewHolder(
		view: View,
		private val clickListener: (viewHolder: OptionViewHolder) -> Unit
	) : RecyclerView.ViewHolder(view), View.OnClickListener {
		val button = view.findViewById<View>(R.id.button) as Checkable
		val title = view.findViewById<TextView>(R.id.title)
		val summary = view.findViewById<TextView>(R.id.summary)
		val container = view.findViewById<ViewGroup>(R.id.container).also {
			it.setOnClickListener(this)
		}

		override fun onClick(view: View) = clickListener(this)
	}

	class CategoryViewHolder(
		view: View
	) : RecyclerView.ViewHolder(view) {
		val title = view.findViewById<TextView>(R.id.title)
	}
}
