package org.jellyfin.androidtv.ui.playback.nextup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentNextUpBinding
import org.jellyfin.androidtv.databinding.FragmentNextUpMinimalBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.toHtmlSpanned
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class NextUpFragment : Fragment() {
	private val viewModel: NextUpViewModel by sharedViewModel()
	private lateinit var binding: FragmentNextUpBinding
	private lateinit var bindingMinimal: FragmentNextUpMinimalBinding
	private val backgroundService: BackgroundService by inject()
	private val userPreferences: UserPreferences by inject()
	private val nextUpFullEnabled = userPreferences[UserPreferences.nextUpFullEnabled]
	private var timerStarted = false

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		if (nextUpFullEnabled) {
			binding = FragmentNextUpBinding.inflate(inflater, container, false)

			binding.fragmentNextUpButtons.apply {
				setPlayNextListener(viewModel::playNext)
				setCancelListener(viewModel::close)
			}
		} else {
			bindingMinimal = FragmentNextUpMinimalBinding.inflate(inflater, container, false)

			bindingMinimal.fragmentNextUpButtons.apply {
				setPlayNextListener(viewModel::playNext)
				setCancelListener(viewModel::close)
			}
		}

		viewModel.item.observe(viewLifecycleOwner) { data ->
			// No data, keep current
			if (data == null) return@observe

			backgroundService.setBackground(data.baseItem)

			if (nextUpFullEnabled) {
				binding.logo.setImageBitmap(data.logo)
				binding.image.setImageBitmap(data.thumbnail)

				binding.title.text = data.title
				binding.description.text = data.description?.toHtmlSpanned()
			} else {
				bindingMinimal.logo.setImageBitmap(data.logo)
				bindingMinimal.title.text = data.title
			}
		}

		return if (nextUpFullEnabled) binding.root else bindingMinimal.root
	}

	override fun onResume() {
		super.onResume()

		if (!timerStarted) {
			if (nextUpFullEnabled) binding.fragmentNextUpButtons.startTimer()
			else bindingMinimal.fragmentNextUpButtons.startTimer()

			timerStarted = true
		}
	}

	override fun onPause() {
		super.onPause()

		if (nextUpFullEnabled) binding.fragmentNextUpButtons.stopTimer()
		else bindingMinimal.fragmentNextUpButtons.stopTimer()
	}
}
