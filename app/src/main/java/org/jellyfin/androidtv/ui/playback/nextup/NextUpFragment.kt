package org.jellyfin.androidtv.ui.playback.nextup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.databinding.FragmentNextUpBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.toHtmlSpanned
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class NextUpFragment : Fragment() {
	private val viewModel: NextUpViewModel by sharedViewModel()
	private lateinit var binding: FragmentNextUpBinding
	private val backgroundService: BackgroundService by inject()
	private val userPreferences: UserPreferences by inject()
	private var timerStarted = false

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val nextUpFullEnabled = userPreferences[UserPreferences.nextUpFullEnabled]
		binding = FragmentNextUpBinding.inflate(inflater, container, false)

		viewModel.item.observe(viewLifecycleOwner) { data ->
			// No data, keep current
			if (data == null) return@observe

			backgroundService.setBackground(data.baseItem)

			if (nextUpFullEnabled) {
				binding.logo.setImageBitmap(data.logo)
				binding.title.text = data.title

				binding.image.setImageBitmap(data.thumbnail)
				binding.description.text = data.description?.toHtmlSpanned()

				binding.nextup.visibility = View.VISIBLE
				binding.nextupMinimal.visibility = View.GONE
			} else {
				binding.logoMinimal.setImageBitmap(data.logo)
				binding.titleMinimal.text = data.title

				binding.nextup.visibility = View.GONE
				binding.nextupMinimal.visibility = View.VISIBLE

				binding.content.background = null;
			}
		}

		binding.fragmentNextUpButtons.apply {
			setPlayNextListener(viewModel::playNext)
			setCancelListener(viewModel::close)
		}

		return binding.root
	}

	override fun onResume() {
		super.onResume()

		if (!timerStarted) {
			binding.fragmentNextUpButtons.startTimer()

			timerStarted = true
		}
	}

	override fun onPause() {
		super.onPause()

		binding.fragmentNextUpButtons.stopTimer()
	}
}
