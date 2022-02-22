package org.jellyfin.androidtv.ui.startup.fragment

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
import kotlinx.coroutines.flow.collect
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.databinding.FragmentSelectServerBinding
import org.jellyfin.androidtv.ui.ServerButtonView
import org.jellyfin.androidtv.ui.SpacingItemDecoration
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.jellyfin.androidtv.util.ListAdapter
import org.jellyfin.androidtv.util.getSummary
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SelectServerFragment : Fragment() {
	private lateinit var binding: FragmentSelectServerBinding
	private val loginViewModel: LoginViewModel by sharedViewModel()

	@Suppress("LongMethod")
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentSelectServerBinding.inflate(inflater, container, false)

		// Create spacing for recycler view of 8dp
		@Suppress("MagicNumber")
		val serverDivider = SpacingItemDecoration(0, 8)

		// Stored servers
		val storedServerAdapter = ServerAdapter { (_, server) ->
			requireActivity()
				.supportFragmentManager
				.commit {
					replace<StartupToolbarFragment>(R.id.content_view)
					add<ServerFragment>(
						R.id.content_view,
						null,
						bundleOf(
							ServerFragment.ARG_SERVER_ID to server.id.toString()
						)
					)
					addToBackStack(null)
				}
		}
		binding.storedServers.setHasFixedSize(true)
		binding.storedServers.addItemDecoration(serverDivider)
		binding.storedServers.adapter = storedServerAdapter

		// Discovery
		binding.discoveryServers.setHasFixedSize(true)
		binding.discoveryServers.addItemDecoration(serverDivider)
		val discoveryServerAdapter = ServerAdapter { (_, server) ->
			loginViewModel.addServer(server.address).observe(viewLifecycleOwner) { state ->
				if (state is ConnectedState) {
					parentFragmentManager.commit {
						replace<StartupToolbarFragment>(R.id.content_view)
						add<ServerFragment>(
							R.id.content_view,
							null,
							bundleOf(
								ServerFragment.ARG_SERVER_ID to state.id.toString()
							)
						)
					}
				} else {
					items = items.map {
						if (it.server.id == server.id) StatefulServer(state, it.server)
						else it
					}

					// Show error as toast
					if (state is UnableToConnectState) {
						Toast.makeText(requireContext(), getString(
							R.string.server_connection_failed_candidates,
							state.addressCandidates
								.map { "${it.key} ${it.value.getSummary(requireContext())}" }
								.joinToString(prefix = "\n", separator = "\n")
						), Toast.LENGTH_LONG).show()
					}
				}
			}
		}
		binding.discoveryServers.adapter = discoveryServerAdapter

		lifecycleScope.launchWhenCreated {
			binding.discoveryProgressIndicator.isVisible = true
			binding.discoveryServers.isVisible = true
			binding.discoveryServers.isFocusable = false

			loginViewModel.storedServers.observe(viewLifecycleOwner) { servers ->
				storedServerAdapter.items = servers.map { StatefulServer(server = it) }

				binding.storedServersTitle.isVisible = servers.isNotEmpty()
				binding.storedServers.isVisible = servers.isNotEmpty()
				binding.storedServers.isFocusable = servers.isNotEmpty()
				binding.welcomeTitle.isVisible = servers.isEmpty()
				binding.welcomeContent.isVisible = servers.isEmpty()

				// Make sure focus is properly set when no servers exist
				if (servers.isEmpty()) binding.enterServerAddress.requestFocus()
			}

			loginViewModel.discoveredServers.collect { server ->
				discoveryServerAdapter.items += StatefulServer(server = server)

				binding.discoveryServers.isFocusable = true
			}

			binding.discoveryProgressIndicator.isVisible = false
			binding.discoveryServers.isVisible = discoveryServerAdapter.itemCount > 0
			binding.discoveryNoneFound.isVisible = discoveryServerAdapter.itemCount == 0
		}

		// Manual
		binding.enterServerAddress.setOnClickListener {
			parentFragmentManager.commit {
				addToBackStack(null)
				replace<AddServerAlertFragment>(R.id.content_view)
			}
		}

		// App info
		@Suppress("SetTextI18n")
		binding.appVersion.text = "jellyfin-androidtv ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"

		// Set focus to fragment
		binding.root.requestFocus()

		return binding.root
	}

	override fun onResume() {
		super.onResume()

		loginViewModel.reloadServers()
	}

	class ServerAdapter(
		var serverClickListener: ServerAdapter.(statefulServer: StatefulServer) -> Unit = {},
	) : ListAdapter<StatefulServer, ServerAdapter.ViewHolder>() {
		override fun areItemsTheSame(old: StatefulServer, new: StatefulServer): Boolean = new.server == old.server

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = ServerButtonView(parent.context).apply {
				layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			}
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, statefulServer: StatefulServer) = with(holder.serverButtonView) {
			val (serverState, server) = statefulServer

			// Set data
			name = server.name
			address = server.address
			version = server.version

			state = when (serverState) {
				is ConnectingState -> ServerButtonView.State.CONNECTING
				is UnableToConnectState -> ServerButtonView.State.ERROR
				else -> ServerButtonView.State.DEFAULT
			}

			// Set actions
			setOnClickListener {
				serverClickListener(statefulServer)
			}
		}

		inner class ViewHolder(
			val serverButtonView: ServerButtonView,
		) : RecyclerView.ViewHolder(serverButtonView)
	}

	data class StatefulServer(val state: ServerAdditionState? = null, val server: Server)
}
