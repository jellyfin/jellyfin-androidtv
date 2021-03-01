package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.*
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber
import java.util.*

class ServerFragment : RowsSupportFragment() {
	companion object {
		const val ARG_SERVER_ID = "server_id"
	}

	private val loginViewModel: LoginViewModel by sharedViewModel()
	private val rowAdapter = MutableObjectAdapter<ListRow>(CustomListRowPresenter())
	private val userComparator = compareByDescending<User> { if (it is PrivateUser) it.lastUsed else 0L }.thenBy { it.name }

	private val itemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
		if (item is UserGridButton) {
			loginViewModel.authenticate(item.user, item.server).observe(viewLifecycleOwner) { state ->
				when (state) {
					AuthenticatingState -> {
						// TODO show Loading state
					}
					RequireSignInState -> {
						// Open login fragment
						navigate(UserLoginAlertFragment(
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
			navigate(UserLoginAlertFragment(
				server = item.server
			))
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		adapter = rowAdapter
		onItemViewClickedListener = itemViewClickedListener

		val serverId = UUID.fromString(arguments?.getString(ARG_SERVER_ID))
		lifecycleScope.launch {
			val server = loginViewModel.getServer(serverId) ?: return@launch
			val users = loginViewModel.getUsers(server).sortedWith(userComparator)

			// Fragment may be unloaded at this point, verify by checking for context
			if (context != null) buildRow(server, users)
		}
	}

	private fun buildRow(server: Server, users: List<User>) {
		Timber.d("Creating server row %s", server.name)

		val userListAdapter = MutableObjectAdapter<GridButton>(GridButtonPresenter())

		users.forEachIndexed { index, user ->
			userListAdapter.add(UserGridButton(
				server = server,
				user = user,
				id = index + 1,
				text = user.name,
				imageId = R.drawable.tile_port_person,
				imageUrl = loginViewModel.getUserImage(server, user),
			))
		}

		userListAdapter.add(AddUserGridButton(
			server = server,
			id = 0,
			text = requireContext().getString(R.string.lbl_manual_login),
			imageId = R.drawable.tile_edit,
		))

		val row = ListRow(
			HeaderItem(server.name.ifBlank { server.address }),
			userListAdapter,
		)

		rowAdapter.add(row)
	}

	override fun onResume() {
		super.onResume()

		// Ensure the server rows get focus
		// FIXME Ideally not done in current fragment as this changes the focus when the screen changes
		requireView().requestFocus()
	}

	private fun navigate(fragment: Fragment) {
		requireActivity()
			.supportFragmentManager
			.beginTransaction()
			.replace(R.id.content_view, fragment)
			.addToBackStack(null)
			.commit()
	}

	private class AddUserGridButton(val server: Server, id: Int, text: String, @DrawableRes imageId: Int) : GridButton(id, text, imageId)
	private class UserGridButton(val server: Server, val user: User, id: Int, text: String, @DrawableRes imageId: Int, imageUrl: String?) : GridButton(id, text, imageId, imageUrl)
}
