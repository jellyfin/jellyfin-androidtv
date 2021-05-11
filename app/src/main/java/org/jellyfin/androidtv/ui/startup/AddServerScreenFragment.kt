package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.databinding.FragmentAddServerScreenBinding
import org.jellyfin.androidtv.databinding.ItemDiscoveryServerBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AddServerScreenFragment : Fragment() {
	private lateinit var binding: FragmentAddServerScreenBinding
	private val loginViewModel: LoginViewModel by sharedViewModel()

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentAddServerScreenBinding.inflate(inflater, container, false)

		// Discovery
		binding.discoveryServers.setHasFixedSize(true)
		val discoveryServerAdapter = DiscoveryServerAdapter { server ->
			requireActivity()
				.supportFragmentManager
				.beginTransaction()
				.replace<AddServerAlertFragment>(
					R.id.content_view,
					null,
					bundleOf(
						AddServerAlertFragment.ARG_SERVER_ADDRESS to server.address
					)
				)
				.addToBackStack(null)
				.commit()
		}
		binding.discoveryServers.adapter = discoveryServerAdapter

		lifecycleScope.launchWhenCreated {
			binding.discoveryProgressIndicator.visibility = View.VISIBLE
			binding.discoveryServers.isFocusable = false

			loginViewModel.discoveredServers.collect { server ->
				discoveryServerAdapter.addServer(server)

				binding.discoveryServers.isFocusable = true
			}

			binding.discoveryProgressIndicator.visibility = View.GONE
			binding.discoveryNoneFound.visibility = if (discoveryServerAdapter.servers.isEmpty()) View.VISIBLE else View.GONE
		}

		// Manual
		binding.enterServerAddress.setOnClickListener { (requireActivity() as StartupActivity).showAddServer() }

		// App info
		binding.appVersion.text = "jellyfin-androidtv ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"

		return binding.root
	}

	class DiscoveryServerAdapter(
		var serverClickListener: (server: Server) -> Unit = {}
	) : RecyclerView.Adapter<DiscoveryServerAdapter.ViewHolder>() {
		private var _servers = mutableListOf<Server>()
		val servers: List<Server> = _servers

		fun addServer(server: Server) {
			_servers.add(server)
			notifyItemInserted(_servers.size - 1)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
			ItemDiscoveryServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		)

		override fun onBindViewHolder(holder: ViewHolder, position: Int) = with(holder.binding) {
			val server = servers[position]

			// Set data
			serverName.text = server.name
			serverAddress.text = server.address

			// FIXME Show server version. It is not exposed with the current apiclient in the DiscoveryServerInfo class
			serverVersion.visibility = View.GONE

			// Set actions
			root.setOnClickListener {
				serverClickListener.invoke(server)
			}
		}

		override fun getItemCount(): Int = servers.size

		inner class ViewHolder(
			val binding: ItemDiscoveryServerBinding
		) : RecyclerView.ViewHolder(binding.root) {
			val root = binding.root
		}
	}
}
