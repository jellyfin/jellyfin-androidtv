package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.AuthenticatedState
import org.jellyfin.androidtv.auth.model.AuthenticatingState
import org.jellyfin.androidtv.auth.model.RequireSignInState
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.ServerUnavailableState
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.androidtv.ui.shared.IFocusListener
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class ListServerFragment : RowsSupportFragment(), IFocusListener {
	private companion object {
		private const val ADD_USER = 1
		private const val SELECT_USER = 2
	}

	private val loginViewModel: LoginViewModel by sharedViewModel()
	private val rowAdapter = MutableObjectAdapter<ListRow>(CustomListRowPresenter())

	private val itemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
		if (item is UserGridButton) {
			loginViewModel.authenticate(item.user, item.server).observe(viewLifecycleOwner) { state ->
				when (state) {
					AuthenticatingState -> {
						// TODO show Loading state
					}
					RequireSignInState -> {
						// Open login fragment
						navigate(UserLoginFragment(
							server = item.server,
							user = item.user,
						))
					}
					ServerUnavailableState -> {
						// TODO show error
					}
					AuthenticatedState -> {
						// TODO use view model and observe in activity or something similar
						(requireActivity() as StartupActivity).openNextActivity()
					}
				}
			}
		} else if (item is AddUserGridButton) {
			// Open login fragment
			navigate(UserLoginFragment(
				server = item.server
			))
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		adapter = rowAdapter
		onItemViewClickedListener = itemViewClickedListener

	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		loginViewModel.servers.observe(viewLifecycleOwner) { servers ->
			buildRows(servers)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return super.onCreateView(inflater, container, savedInstanceState)?.apply {
			updatePadding(top = 20)
		}
	}

	private fun buildRows(servers: Map<Server, Set<User>>) {
		servers.forEach { (server, users) ->
			// Convert the UUID of the server to a long to get a unique id
			// to make sure a server can't be added multiple times
			val uniqueRowId = server.id.mostSignificantBits and Long.MAX_VALUE
			val exists = rowAdapter.any { it.id == uniqueRowId }
			// Already added, don't add it again
			if (exists) return@forEach

			Timber.d("Creating server row %s", server.name)

			val userListAdapter = MutableObjectAdapter<GridButton>(GridButtonPresenter())

			users.forEach { user ->
				userListAdapter.add(UserGridButton(server, user, SELECT_USER, user.name, R.drawable.tile_port_person, loginViewModel.getUserImage(server, user)))
			}

			userListAdapter.add(AddUserGridButton(server, ADD_USER, requireContext().getString(R.string.lbl_manual_login), R.drawable.tile_edit))

			val row = ListRow(
				uniqueRowId,
				HeaderItem(if (server.name.isNotBlank()) server.name else server.address),
				userListAdapter
			)

			rowAdapter.add(row)
		}

		// Ensure the server rows get focus
		requireView().requestFocus()
	}

	private fun navigate(fragment: Fragment) {
		parentFragmentManager.beginTransaction()
			.replace(R.id.content_view, fragment)
			.addToBackStack(this::class.simpleName)
			.commit()
	}

	private class AddUserGridButton(val server: Server, id: Int, text: String, @DrawableRes imageId: Int) : GridButton(id, text, imageId)

	private class UserGridButton(val server: Server, val user: User, id: Int, text: String, @DrawableRes imageId: Int, imageUrl: String?) : GridButton(id, text, imageId, imageUrl)

	override fun onWindowFocusChanged(hasFocus: Boolean) {
		if (hasFocus)
			loginViewModel.refreshServers()
	}
}
