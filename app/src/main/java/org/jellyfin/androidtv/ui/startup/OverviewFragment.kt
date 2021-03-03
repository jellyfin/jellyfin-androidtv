package org.jellyfin.androidtv.ui.startup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.databinding.FragmentServerListBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class OverviewFragment : Fragment() {
	private val loginViewModel: LoginViewModel by sharedViewModel()
	private lateinit var binding: FragmentServerListBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = FragmentServerListBinding.inflate(inflater, container, false)

		// Create adapter for screens
		val serverAdapter = ServerAdapter(requireContext(), childFragmentManager)
		binding.serverView.adapter = serverAdapter

		// Show server list after loading so the "add server" fragment doesn't pop up
		binding.serverView.visibility = View.GONE
		loginViewModel.storedServers.observe(viewLifecycleOwner) { servers ->
			binding.serverView.visibility = View.VISIBLE
			serverAdapter.servers = servers
		}

		return binding.root
	}

	private class ServerAdapter(
		private val context: Context,
		fragmentManager: FragmentManager,
	) : FragmentStatePagerAdapter(
		fragmentManager,
		BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT,
	) {
		private val comparator = compareByDescending<Server> { it.dateLastAccessed }.thenBy { it.name }
		private var _servers = emptyList<Server>()
		var servers
			set(value) {
				_servers = value.sortedWith(comparator)
				notifyDataSetChanged()
			}
			get() = _servers

		override fun getCount() = servers.size + 1

		override fun getItem(position: Int) = when {
			// Last page is always used to add servers
			position == servers.size -> AddServerScreenFragment()
			else -> ServerFragment().apply {
				val server = servers[position]

				arguments = bundleOf(
					ServerFragment.ARG_SERVER_ID to server.id.toString(),
				)
			}
		}

		override fun getPageTitle(position: Int) = when {
			// Last page is always used to add servers
			position == servers.size -> context.getString(R.string.connect_title)
			else -> servers.getOrNull(position)?.name
		}
	}
}

