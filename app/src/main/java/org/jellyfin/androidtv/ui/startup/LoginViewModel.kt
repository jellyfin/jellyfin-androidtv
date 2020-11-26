package org.jellyfin.androidtv.ui.startup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import org.jellyfin.androidtv.data.model.Server
import org.jellyfin.androidtv.data.model.User
import org.jellyfin.androidtv.data.repository.ServerRepository
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class LoginViewModel(
	serverRepository: ServerRepository,
) : ViewModel() {
	// All available servers and users
	private val _servers = serverRepository.getServersWithUsers(
		discovery = true,
		stored = true,
		legacy = true
	).asLiveDataCollection()
	val servers: LiveData<List<Pair<Server, List<User>>>> get() = _servers

	// TODO move to utils
	private fun <T> Flow<T>.asLiveDataCollection(
		context: CoroutineContext = EmptyCoroutineContext
	): LiveData<List<T>> {
		val list = mutableListOf<T>()
		val liveData = MediatorLiveData<List<T>>()

		liveData.addSource(asLiveData(context)) {
			list.add(it)
			liveData.value = list
		}

		return liveData
	}

	suspend fun connect(address: String) {
		// TODO
	}

	suspend fun login(userId: UUID) {
		TODO()
	}

	suspend fun login(serverId: UUID, username: String, password: String) {
		TODO()
	}
}
