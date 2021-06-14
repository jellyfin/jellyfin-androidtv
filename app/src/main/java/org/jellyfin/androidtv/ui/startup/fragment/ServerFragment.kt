package org.jellyfin.androidtv.ui.startup.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.ServerRepository
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.databinding.FragmentServerBinding
import org.jellyfin.androidtv.ui.ServerButtonView
import org.jellyfin.androidtv.ui.card.DefaultCardView
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.jellyfin.androidtv.util.ListAdapter
import org.jellyfin.androidtv.util.sdk.asText
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ServerFragment : Fragment() {
	companion object {
		const val ARG_SERVER_ID = "server_id"
	}

	private val loginViewModel: LoginViewModel by sharedViewModel()
	private lateinit var binding: FragmentServerBinding

	private val serverIdArgument get() = arguments?.getString(ARG_SERVER_ID)?.ifBlank { null }?.toUUIDOrNull()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		val server = serverIdArgument?.let(loginViewModel::getServer)

		if (server == null) {
			navigateFragment<SelectServerFragment>(keepToolbar = true, keepHistory = false)
			return null
		}

		binding = FragmentServerBinding.inflate(inflater, container, false)

		val userAdapter = UserAdapter(requireContext(), server, loginViewModel)
		userAdapter.onItemPressed = { user ->
			loginViewModel.authenticate(user, server).observe(viewLifecycleOwner) { state ->
				when (state) {
					// Ignored states
					AuthenticatingState -> Unit
					AuthenticatedState -> Unit
					// Actions
					RequireSignInState -> navigateFragment<UserLoginAlertFragment>(bundleOf(
						UserLoginAlertFragment.ARG_SERVER_ID to server.id.toString(),
						UserLoginAlertFragment.ARG_USERNAME to user.name,
					))
					// Errors
					ServerUnavailableState -> Toast.makeText(context, R.string.server_connection_failed, Toast.LENGTH_LONG).show()
					is ServerVersionNotSupported -> Toast.makeText(
						context,
						getString(R.string.server_unsupported, state.server.version, ServerRepository.minimumServerVersion.asText()),
						Toast.LENGTH_LONG
					).show()
				}
			}
		}
		binding.users.adapter = userAdapter

		loginViewModel.getUsers(server).observe(viewLifecycleOwner) { users ->
			userAdapter.items = users

			binding.users.isFocusable = users.any()
			binding.noUsersWarning.isVisible = users.isEmpty()
			binding.root.requestFocus()
		}

		onServerChange(server)

		lifecycleScope.launchWhenCreated {
			val updated = loginViewModel.updateServer(server)
			if (updated) loginViewModel.getServer(server.id)?.let(::onServerChange)
		}

		return binding.root
	}

	private fun onServerChange(server: Server) {
		binding.loginDisclaimer.text = server.loginDisclaimer

		binding.serverButton.apply {
			state = ServerButtonView.State.EDIT
			name = server.name
			address = server.address
			version = server.version
		}

		binding.addUserButton.setOnClickListener {
			navigateFragment<UserLoginAlertFragment>(
				args = bundleOf(
					UserLoginAlertFragment.ARG_SERVER_ID to server.id.toString(),
					UserLoginAlertFragment.ARG_USERNAME to null
				)
			)
		}

		binding.serverButton.setOnClickListener {
			navigateFragment<SelectServerFragment>(keepToolbar = true)
		}
	}

	private inline fun <reified F : Fragment> navigateFragment(
		args: Bundle = bundleOf(),
		keepToolbar: Boolean = false,
		keepHistory: Boolean = true,
	) {
		requireActivity()
			.supportFragmentManager
			.commit {
				if (keepToolbar) {
					replace<StartupToolbarFragment>(R.id.content_view)
					add<F>(R.id.content_view, null, args)
				} else {
					replace<F>(R.id.content_view, null, args)
				}

				if (keepHistory) addToBackStack(null)
			}
	}

	override fun onResume() {
		super.onResume()

		loginViewModel.reloadServers()
	}

	private class UserAdapter(
		private val context: Context,
		private val server: Server,
		private val loginViewModel: LoginViewModel,
	) : ListAdapter<User, UserAdapter.ViewHolder>() {
		var onItemPressed: (User) -> Unit = {}

		override fun areItemsTheSame(old: User, new: User): Boolean = old.id == new.id

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val cardView = DefaultCardView(context).apply {
				setSize(DefaultCardView.Size.SQUARE)
			}

			return ViewHolder(cardView)
		}

		override fun onBindViewHolder(holder: ViewHolder, user: User) {
			holder.cardView.title = user.name
			holder.cardView.setImage(loginViewModel.getUserImage(server, user), R.drawable.tile_port_person)

			holder.cardView.setOnClickListener {
				onItemPressed(user)
			}
		}

		private class ViewHolder(
			val cardView: DefaultCardView,
		) : RecyclerView.ViewHolder(cardView)
	}
}

