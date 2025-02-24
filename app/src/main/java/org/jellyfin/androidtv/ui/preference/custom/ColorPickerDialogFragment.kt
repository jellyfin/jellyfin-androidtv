package org.jellyfin.androidtv.ui.preference.custom

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.leanback.preference.LeanbackPreferenceDialogFragmentCompat
import androidx.leanback.widget.VerticalGridView
import androidx.preference.ListPreference
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.databinding.PreferenceColorListBinding

class ColorPickerDialogFragment : LeanbackPreferenceDialogFragmentCompat() {
	companion object {
		fun newInstance(key: String) = ColorPickerDialogFragment().apply {
			arguments = Bundle().apply {
				putString(ARG_KEY, key)
			}
		}
	}

	private var _binding: PreferenceColorListBinding? = null
	private val binding get() = _binding!!
	private lateinit var adapter: RecyclerView.Adapter<*>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val colorListPreference = preference as? ColorListPreference ?: throw NotImplementedError()

		adapter = Adapter(
			items = colorListPreference.items,
			selectedValue = colorListPreference.value
		)
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val styledContext = ContextThemeWrapper(activity, androidx.leanback.preference.R.style.PreferenceThemeOverlayLeanback)
		val styledInflater = inflater.cloneInContext(styledContext)
		_binding = PreferenceColorListBinding.inflate(styledInflater, container, false)

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
	data class ColorListItem(
		val key: Color,
		val title: String,
		val summary: String? = null
	)

	inner class Adapter(
		private val items: List<ColorListItem>,
		private var selectedValue: String? = null,
	) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
		override fun getItemCount() = items.size

		override fun getItemViewType(position: Int) = R.layout.preference_color_list_option

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			val inflater = LayoutInflater.from(parent.context)
			val view = inflater.inflate(viewType, parent, false)
			return OptionViewHolder(view, ::onItemClick)
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			holder as OptionViewHolder

			val item = items[position]
			holder.button.isChecked = item.key.toArgb().toString() == selectedValue

			val buttontint = when {
				holder.button.isChecked -> R.color.button_default_normal_text
				else -> R.color.transparent
			}
			holder.buttonbg.background = context?.let {
				ResourcesCompat.getDrawable(it.resources, R.drawable.subtitle_background, it.theme)
			}
			holder.buttonfg.backgroundTintList = ColorStateList.valueOf(item.key.toArgb())
			holder.buttontint.buttonTintList =
				context?.let { context ->
					ContextCompat.getColor(context, buttontint).let {
						ColorStateList.valueOf(it)
					}
				}
			holder.title.text = item.title
			holder.summary.text = item.summary
			holder.summary.isVisible = item.summary?.isNotBlank() == true
		}

		fun onItemClick(viewHolder: OptionViewHolder) {
			val index = viewHolder.absoluteAdapterPosition
			if (index == RecyclerView.NO_POSITION) return

			val item = items[index]
			if (preference.callChangeListener(item.key)) {
				when (val preference = preference) {
					is ListPreference -> preference.value = item.key.toArgb().toString()
				}

				selectedValue = item.key.toArgb().toString()
			}

			parentFragmentManager.popBackStack()
			notifyDataSetChanged()
		}
	}

	class OptionViewHolder(
		view: View,
		private val clickListener: (viewHolder: OptionViewHolder) -> Unit
	) : RecyclerView.ViewHolder(view), View.OnClickListener {
		var buttonbg: View = view.findViewById(R.id.button)
		var buttonfg: View = view.findViewById(R.id.button)
		var buttontint: RadioButton = view.findViewById(R.id.button)
		val button = view.findViewById(R.id.button) as Checkable
		val title: TextView = view.findViewById(R.id.title)
		val summary: TextView = view.findViewById(R.id.summary)
		val container: ViewGroup = view.findViewById<ViewGroup>(R.id.container).also {
			it.setOnClickListener(this)
		}

		override fun onClick(view: View) = clickListener(this)
	}
}
