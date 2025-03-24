package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.ConnectedState
import org.jellyfin.androidtv.auth.model.ConnectingState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerAdditionState
import org.jellyfin.androidtv.auth.model.UnableToConnectState
import org.jellyfin.androidtv.data.repository.NotificationsRepository
import org.jellyfin.androidtv.databinding.FragmentSelectServerBinding
import org.jellyfin.androidtv.ui.ServerButtonView
import org.jellyfin.androidtv.ui.SpacingItemDecoration
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.startup.StartupViewModel
import org.jellyfin.androidtv.util.ListAdapter
import org.jellyfin.androidtv.util.MenuBuilder
import org.jellyfin.androidtv.util.getSummary
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.compose.koinInject

class SelectServerFragment : Fragment() {
	private var _binding: FragmentSelectServerBinding? = null
	private val binding get() = _binding!!
	private val startupViewModel: StartupViewModel by activityViewModel()

	@Suppress("LongMethod")
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		_binding = FragmentSelectServerBinding.inflate(inflater, container, false)

		// Create spacing for recycler view of 8dp
		@Suppress("MagicNumber")
		val serverDivider = SpacingItemDecoration(0, 8)

		// Stored servers
		val storedServerAdapter = ServerAdapter(
			serverClickListener = { (_, server) ->
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
			},
			serverPopupBuilder = { server ->
				item(getString(R.string.lbl_remove)) {
					startupViewModel.deleteServer(server.id)
				}
			}
		)
		binding.storedServers.setHasFixedSize(true)
		binding.storedServers.addItemDecoration(serverDivider)
		binding.storedServers.adapter = storedServerAdapter

		// Discovery
		binding.discoveryServers.setHasFixedSize(true)
		binding.discoveryServers.addItemDecoration(serverDivider)
		val discoveryServerAdapter = ServerAdapter(
			serverClickListener = { (_, server) ->
				startupViewModel.addServer(server.address).onEach { state ->
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
				}.launchIn(lifecycleScope)
			}
		)
		binding.discoveryServers.adapter = discoveryServerAdapter

		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				binding.discoveryProgressIndicator.isVisible = true
				binding.discoveryServers.isVisible = true
				binding.discoveryServers.isFocusable = false

				startupViewModel.storedServers.onEach { servers ->
					storedServerAdapter.items = servers.map { StatefulServer(server = it) }

					binding.storedServersTitle.isVisible = servers.isNotEmpty()
					binding.storedServers.isVisible = servers.isNotEmpty()
					binding.storedServers.isFocusable = servers.isNotEmpty()
					binding.welcomeTitle.isVisible = servers.isEmpty()
					binding.welcomeContent.isVisible = servers.isEmpty()

					// Make sure focus is properly set when no servers exist
					if (servers.isEmpty()) binding.enterServerAddress.requestFocus()
				}.launchIn(this)

				startupViewModel.discoveredServers.onEach { servers ->
					discoveryServerAdapter.items = servers.map { StatefulServer(server = it) }

					binding.discoveryServers.isFocusable = servers.any()
					binding.discoveryServers.isVisible = discoveryServerAdapter.itemCount > 0
					binding.discoveryNoneFound.isVisible = discoveryServerAdapter.itemCount == 0
				}.launchIn(this)

				binding.discoveryProgressIndicator.isVisible = false
			}
		}

		// Notifications
		binding.notifications.setContent {
			val notificationsRepository = koinInject<NotificationsRepository>()
			val notifications by notificationsRepository.notifications.collectAsState()

			Column(
				verticalArrangement = Arrangement.spacedBy(5.dp)
			) {
				for (notification in notifications) {
					if (!notification.public) continue

					Box(
						modifier = Modifier
							.fillMaxWidth()
							.clip(JellyfinTheme.shapes.medium)
							.background(colorResource(id = R.color.lb_basic_card_info_bg_color)),
					) {
						Text(
							text = notification.message,
							modifier = Modifier.padding(10.dp),
							color = colorResource(id = R.color.white),
						)
					}
				}
			}
		}

		// Manual
		binding.enterServerAddress.setOnClickListener {
			parentFragmentManager.commit {
				addToBackStack(null)
				replace<ServerAddFragment>(R.id.content_view)
			}
		}

		// App info
		@Suppress("SetTextI18n")
		binding.appVersion.text = "jellyfin-androidtv ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"

		// Set focus to fragment
		binding.root.requestFocus()

		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()

		_binding = null
	}

	override fun onResume() {
		super.onResume()

		startupViewModel.reloadStoredServers()
		startupViewModel.loadDiscoveryServers()
	}

	class ServerAdapter(
		var serverClickListener: ServerAdapter.(statefulServer: StatefulServer) -> Unit = {},
		var serverPopupBuilder: MenuBuilder.(server: Server) -> Unit = {},
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
			setOnClickListener { serverClickListener(statefulServer) }
			setPopupMenu { serverPopupBuilder(server) }
		}

		inner class ViewHolder(
			val serverButtonView: ServerButtonView,
		) : RecyclerView.ViewHolder(serverButtonView)
	}

	data class StatefulServer(val state: ServerAdditionState? = null, val server: Server)
}
