package org.jellyfin.androidtv.ui.home

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.databinding.FragmentToolbarHomeBinding
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.search.SearchActivity
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.jellyfin.androidtv.util.ImageUtils
import org.koin.android.ext.android.inject

class HomeToolbarFragment : Fragment() {
	private lateinit var binding: FragmentToolbarHomeBinding
	private val sessionRepository by inject<SessionRepository>()
	private val userRepository by inject<UserRepository>()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentToolbarHomeBinding.inflate(inflater, container, false)

		binding.settings.setOnClickListener {
			val settingsIntent = Intent(activity, PreferencesActivity::class.java)
			activity?.startActivity(settingsIntent)
		}

		binding.switchUsers.setOnClickListener {
			switchUser()
		}

		binding.search.setOnClickListener {
			val settingsIntent = Intent(activity, SearchActivity::class.java)
			activity?.startActivity(settingsIntent)
		}

		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		viewLifecycleOwner.lifecycleScope.launchWhenCreated {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				userRepository.currentUser.collect { user ->
					if (user != null) {
						val image = ImageUtils.getPrimaryImageUrl(user)
						setUserImage(image)
					}
				}
			}
		}
	}

	private fun setUserImage(image: String?) {
		Glide.with(requireContext())
			.load(image)
			.placeholder(R.drawable.ic_switch_users)
			.centerInside()
			.circleCrop()
			.into(object : CustomViewTarget<ImageButton, Drawable>(binding.switchUsers) {
				override fun onLoadFailed(errorDrawable: Drawable?) {
					binding.switchUsers.imageTintMode = PorterDuff.Mode.SRC_IN
					binding.switchUsers.setImageDrawable(errorDrawable)
				}

				override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
					binding.switchUsers.imageTintMode = null
					binding.switchUsers.setImageDrawable(resource)
				}

				override fun onResourceCleared(placeholder: Drawable?) {
					binding.switchUsers.imageTintMode = PorterDuff.Mode.SRC_IN
					binding.switchUsers.setImageDrawable(placeholder)
				}
			})
	}

	private fun switchUser() {
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
