package org.jellyfin.androidtv.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.Observer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.ServerList
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.ui.presentation.CustomListRowPresenter
import org.jellyfin.androidtv.ui.presentation.GridButtonPresenter
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

class ListServerFragment : RowsSupportFragment() {
	private companion object {
		private const val ADD_USER = 1
		private const val SELECT_USER = 2
	}

	private val loginViewModel: LoginViewModel by sharedViewModel()

	private val itemViewClickedListener = OnItemViewClickedListener() { _: Presenter.ViewHolder, item: Any, _: RowPresenter.ViewHolder, _: Row ->
		if (item is UserGridButton) {
			if (item.user.hasPassword) {
				// Open login fragment
				navigate(UserLoginFragment(
					user = item.user,
					onConfirmCallback = { username: String, password: String ->
						GlobalScope.launch {
							loginViewModel.login(server = item.server, username = username, password = password)
						}
					},
					onCancelCallback = { parentFragmentManager.popBackStack() }
				))
			} else {
				GlobalScope.launch {
					loginViewModel.login(item.server, item.user.name, "")
				}
			}
		} else if (item is AddUserGridButton) {
			// Open login fragment
			navigate(UserLoginFragment(
				onConfirmCallback = { username: String, password: String ->
					GlobalScope.launch {
						loginViewModel.login(server = item.server, username = username, password = password)
					}
				},
				onCancelCallback = { parentFragmentManager.popBackStack() }
			))
		}
	}

	override fun onActivityCreated(savedInstanceState: Bundle?) {
		super.onActivityCreated(savedInstanceState)

		buildRows(emptyMap())

		val serverObserver = Observer<ServerList> { serverList ->
			if (serverList.allServersUsers.isNotEmpty()) buildRows(serverList.allServersUsers)
		}

		loginViewModel.serverList.observe(viewLifecycleOwner, serverObserver)

		onItemViewClickedListener = itemViewClickedListener
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return super.onCreateView(inflater, container, savedInstanceState)?.apply {
			updatePadding(top = 20)
		}
	}

	private fun buildRows(usersByServer: Map<Server, List<User>>) {
		val rowAdapter = ArrayObjectAdapter(CustomListRowPresenter())
		adapter = rowAdapter

		usersByServer.forEach { (server: Server, userList: List<User>) ->
			Timber.d("Adding server row %s", server.name)

			val userListAdapter = ArrayObjectAdapter(GridButtonPresenter())
			userList.forEach { user ->
				userListAdapter.add(UserGridButton(server, user, SELECT_USER, user.name, R.drawable.tile_port_person))
			}

			userListAdapter.add(AddUserGridButton(server, ADD_USER, requireContext().getString(R.string.lbl_manual_login), R.drawable.tile_edit))

			rowAdapter.add(ListRow(
				HeaderItem(usersByServer.keys.indexOf(server).toLong(),
						   if (server.name.isNotBlank()) server.name else server.address),
				userListAdapter
			))
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

	private class UserGridButton(val server: Server, val user: User, id: Int, text: String, @DrawableRes imageId: Int) : GridButton(id, text, imageId)
}
