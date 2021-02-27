package org.jellyfin.androidtv.ui.home

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.auth.SessionRepository
import org.jellyfin.androidtv.databinding.FragmentToolbarHomeBinding
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.search.SearchActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.ImageUtils
import org.jellyfin.apiclient.interaction.ApiClient
import org.koin.android.ext.android.inject

class HomeToolbarFragment : Fragment() {
	private lateinit var binding: FragmentToolbarHomeBinding
	private val apiClient: ApiClient by inject()
	private val sessionRepository: SessionRepository by inject()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentToolbarHomeBinding.inflate(inflater, container, false)

		TvApp.getApplication().currentUserLiveData.observe(viewLifecycleOwner) { currentUser ->
			val image = currentUser?.let { ImageUtils.getPrimaryImageUrl(it, apiClient) }
			setUserImage(image)
		}

		binding.settings.setOnClickListener {
			val settingsIntent = Intent(activity, PreferencesActivity::class.java)
			activity?.startActivity(settingsIntent)
		}

		binding.switchUsersContainer.setOnClickListener {
			switchUser()
		}

		binding.search.setOnClickListener {
			val settingsIntent = Intent(activity, SearchActivity::class.java)
			activity?.startActivity(settingsIntent)
		}

		return binding.root
	}

	private fun setUserImage(image: String?) {
		Glide.with(requireContext())
			.load(image)
			.centerInside()
			.circleCrop()
			.into(object : CustomViewTarget<ImageButton, Drawable>(binding.switchUsersImage) {
				override fun onLoadFailed(errorDrawable: Drawable?) {
					binding.switchUsersImage.isVisible = false
					binding.switchUsersIcon.isVisible = true
					binding.switchUsersImage.setImageDrawable(null)
				}

				override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
					binding.switchUsersImage.isVisible = true
					binding.switchUsersIcon.isVisible = false
					binding.switchUsersImage.setImageDrawable(resource)
				}

				override fun onResourceCleared(placeholder: Drawable?) {
					binding.switchUsersImage.isVisible = false
					binding.switchUsersIcon.isVisible = true
					binding.switchUsersImage.setImageDrawable(null)
				}
			})
	}

	private fun switchUser() {
		// Stop observer so the current image is kept during the transition
		TvApp.getApplication().currentUserLiveData.removeObservers(viewLifecycleOwner)

		sessionRepository.destroyCurrentSession()

		// Open login activity
		val selectUserIntent = Intent(activity, StartupActivity::class.java)
		selectUserIntent.putExtra(StartupActivity.EXTRA_HIDE_SPLASH, true)
		// Remove history to prevent user going back to current activity
		selectUserIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

		activity?.startActivity(selectUserIntent)
		activity?.finishAfterTransition()
	}
}
