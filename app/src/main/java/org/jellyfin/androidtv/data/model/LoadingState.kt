package org.jellyfin.androidtv.data.model

data class LoadingState private constructor(val status: Status, val message: String? = null){
	companion object {
		val PENDING = LoadingState(Status.PENDING)
		val LOADING = LoadingState(Status.LOADING)
		val SUCCESS = LoadingState(Status.SUCCESS)
		fun error(msg: String? = null) = LoadingState(Status.ERROR, msg)
	}

	enum class Status {
		PENDING,
		LOADING,
		SUCCESS,
		ERROR
	}
}
