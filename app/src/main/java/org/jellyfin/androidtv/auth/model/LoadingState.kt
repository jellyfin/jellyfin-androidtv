package org.jellyfin.androidtv.auth.model

import androidx.annotation.StringRes

data class LoadingState(
	val status: Status,
	@StringRes val messageRes: Int? = null
) {
	companion object {
		val PENDING = LoadingState(Status.PENDING)
		val LOADING = LoadingState(Status.LOADING)
		val SUCCESS = LoadingState(Status.SUCCESS)
		val ERROR = LoadingState(Status.ERROR)
		fun error(@StringRes messageRes: Int? = null) = LoadingState(Status.ERROR, messageRes)
	}

	enum class Status {
		PENDING,
		LOADING,
		SUCCESS,
		ERROR
	}
}
