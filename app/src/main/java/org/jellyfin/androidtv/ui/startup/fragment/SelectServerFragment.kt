package org.jellyfin.androidtv.ui.startup.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.databinding.FragmentSelectServerBinding
import org.jellyfin.androidtv.ui.ServerButtonView
import org.jellyfin.androidtv.ui.SpacingItemDecoration
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.preference.screen.AuthPreferencesScreen
import org.jellyfin.androidtv.ui.startup.LoginViewModel
import org.jellyfin.androidtv.util.ListAdapter
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
		val storedServerAdapter = ServerAdapter { server ->
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
		val discoveryServerAdapter = ServerAdapter { server ->
			requireActivity()
				.supportFragmentManager
				.commit {
					replace<AddServerAlertFragment>(
						R.id.content_view,
						null,
						bundleOf(
							AddServerAlertFragment.ARG_SERVER_ADDRESS to server.address
						)
					)
					addToBackStack(null)
				}
		}
		binding.discoveryServers.adapter = discoveryServerAdapter

		lifecycleScope.launchWhenCreated {
			binding.discoveryProgressIndicator.isVisible = true
			binding.discoveryServers.isVisible = true
			binding.discoveryServers.isFocusable = false

			loginViewModel.storedServers.observe(viewLifecycleOwner) { servers ->
				storedServerAdapter.items = servers

				binding.storedServersTitle.isVisible = servers.any()
				binding.storedServers.isVisible = servers.any()
				binding.storedServers.isFocusable = servers.any()
				binding.welcomeTitle.isVisible = servers.isEmpty()
				binding.welcomeContent.isVisible = servers.isEmpty()
			}

			loginViewModel.discoveredServers.collect { server ->
				discoveryServerAdapter.items += server

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
		binding.manageServers.setOnClickListener {
			val intent = Intent(requireActivity(), PreferencesActivity::class.java)
			intent.putExtra(PreferencesActivity.EXTRA_SCREEN, AuthPreferencesScreen::class.qualifiedName)
			startActivity(intent)
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
		var serverClickListener: (server: Server) -> Unit = {},
	) : ListAdapter<Server, ServerAdapter.ViewHolder>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = ServerButtonView(parent.context).apply {
				layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			}
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, server: Server) = with(holder.serverButtonView) {
			// Set data
			name = server.name
			address = server.address
			version = server.version

			// Set actions
			setOnClickListener {
				serverClickListener.invoke(server)
			}
		}

		inner class ViewHolder(
			val serverButtonView: ServerButtonView,
		) : RecyclerView.ViewHolder(serverButtonView)
	}
}
