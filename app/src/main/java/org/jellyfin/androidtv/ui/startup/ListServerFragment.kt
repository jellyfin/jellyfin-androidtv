package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.repository.AuthenticatedState
import org.jellyfin.androidtv.data.repository.AuthenticatingState
import org.jellyfin.androidtv.data.repository.RequireSignInState
import org.jellyfin.androidtv.data.repository.ServerUnavailableState
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.jellyfin.androidtv.util.toUUID
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ListServerFragment : RowsSupportFragment() {
	private companion object {
		private const val ADD_USER = 1
		private const val SELECT_USER = 2
	}

	private val loginViewModel: LoginViewModel by viewModel()

	private val itemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
		if (item is UserGridButton) {
			loginViewModel.authenticate(item.user.id.toUUID()).observe(viewLifecycleOwner) { state ->
				when (state) {
					AuthenticatingState -> {
					}
					RequireSignInState -> {
						// Open login fragment
						navigate(UserLoginFragment(
							serverId = item.server.id.toUUID(),
							user = item.user,
						))
					}
					ServerUnavailableState -> {
						// TODO show error
					}
					AuthenticatedState -> {
						// TODO use view model and observe in activity
						(requireActivity() as StartupActivity).openNextActivity()
					}
				}
			}
		} else if (item is AddUserGridButton) {
			// Open login fragment
			navigate(UserLoginFragment(
				serverId = item.server.id.toUUID()
			))
		}
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		buildRows(emptyList())

		loginViewModel.servers.observe(viewLifecycleOwner) { servers ->
			buildRows(servers)
		}

		onItemViewClickedListener = itemViewClickedListener
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return super.onCreateView(inflater, container, savedInstanceState)?.apply {
			updatePadding(top = 20)
		}
	}

	private fun buildRows(servers: List<Pair<Server, List<User>>>) {
		val rowAdapter = ArrayObjectAdapter(CustomListRowPresenter())

		servers.forEachIndexed { index, (server: Server, userList: List<User>) ->
			Timber.d("Adding server row %s", server.name)

			val userListAdapter = ArrayObjectAdapter(GridButtonPresenter())
			userList.forEach { user ->
				userListAdapter.add(UserGridButton(server, user, SELECT_USER, user.name, R.drawable.tile_port_person))
			}

			userListAdapter.add(AddUserGridButton(server, ADD_USER, requireContext().getString(R.string.lbl_manual_login), R.drawable.tile_edit))

			rowAdapter.add(ListRow(
				HeaderItem(index.toLong(),
					if (server.name.isNotBlank()) server.name else server.address),
				userListAdapter
			))
		}

		adapter = rowAdapter
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

	private class UserGridButton(val server: Server, val user: User, id: Int, text: String, @DrawableRes imageId: Int) : GridButton(id, text, imageId)
}
