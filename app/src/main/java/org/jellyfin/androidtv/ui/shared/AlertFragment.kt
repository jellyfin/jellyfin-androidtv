package org.jellyfin.androidtv.ui.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.databinding.FragmentAlertDialogBinding

abstract class AlertFragment : Fragment() {
	@StringRes
	protected var title: Int? = null

	private lateinit var binding: FragmentAlertDialogBinding
	protected val parentBinding get() = binding

	@CallSuper
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentAlertDialogBinding.inflate(inflater, container, false)
		onCreateChildView(inflater, binding.content)?.let { childView ->
			binding.content.addView(childView)
		}
		return binding.root
	}

	@CallSuper
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		title?.let { binding.title.setText(it) }

		with(binding.confirm) {
			requestFocus()
			setOnClickListener {
				if (onConfirm()) onClose()
			}
		}

		binding.cancel.setOnClickListener {
			if (onCancel()) onClose()
		}
	}

	abstract fun onCreateChildView(inflater: LayoutInflater, contentContainer: ViewGroup): View?

	open fun onConfirm(): Boolean = true
	open fun onCancel(): Boolean = true
	open fun onClose() = parentFragmentManager.popBackStack()
}
