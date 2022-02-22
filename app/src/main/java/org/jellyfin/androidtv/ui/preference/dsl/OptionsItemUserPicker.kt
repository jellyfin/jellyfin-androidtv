package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.AuthenticationRepository
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem.RichListOption
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem.RichListSection
import org.jellyfin.androidtv.ui.preference.custom.RichListPreference
import java.text.DateFormat
import java.util.UUID

class OptionsItemUserPicker(
	private val context: Context,
	private val authenticationRepository: AuthenticationRepository,
) : OptionsItemMutable<OptionsItemUserPicker.UserSelection>() {
	var dialogMessage: String? = null
	var allowDisable: Boolean = true
	var allowLatest: Boolean = true

	fun setTitle(@StringRes resId: Int) {
		title = context.getString(resId)
	}

	fun setDialogMessage(@StringRes resId: Int?) {
		dialogMessage = resId?.let(context::getString)
	}

	private fun MutableList<RichListItem<UserSelection>>.add(
		behavior: UserSelectBehavior,
		userId: UUID? = null,
		title: String,
		summary: String
	) = add(
		RichListOption(
			UserSelection(
				behavior = behavior,
				userId = userId
			),
			title,
			summary
		)
	)

	@ExperimentalStdlibApi
	private fun createItems() = buildList {
		// Add special behaviors
		if (allowDisable) add(
			behavior = UserSelectBehavior.DISABLED,
			title = context.getString(R.string.user_picker_disable_title),
			summary = context.getString(R.string.user_picker_disable_summary)
		)

		if (allowLatest) add(
			behavior = UserSelectBehavior.LAST_USER,
			title = context.getString(R.string.user_picker_last_user_title),
			summary = context.getString(R.string.user_picker_last_user_summary)
		)

		// Add users grouped by server
		for (server in authenticationRepository.getServers()) {
			val users = authenticationRepository.getUsers(server.id)
			if (users.isNullOrEmpty()) continue

			add(RichListSection(server.name))

			for (user in users) add(
				behavior = UserSelectBehavior.SPECIFIC_USER,
				userId = user.id,
				title = user.name,
				summary = context.getString(
					R.string.lbl_user_last_used,
					DateFormat.getDateInstance(DateFormat.MEDIUM).format(user.lastUsed),
					DateFormat.getTimeInstance(DateFormat.SHORT).format(user.lastUsed)
				)
			)
		}
	}

	@OptIn(ExperimentalStdlibApi::class)
	override fun build(category: PreferenceCategory, container: OptionsUpdateFunContainer) {
		val pref = RichListPreference<UserSelection>(context).also {
			it.isPersistent = false
			it.key = UUID.randomUUID().toString()
			category.addPreference(it)
			it.isEnabled = dependencyCheckFun() && enabled
			it.isVisible = visible
			it.title = title
			it.dialogTitle = title
			it.dialogMessage = dialogMessage
			it.summaryProvider = RichListPreference.SimpleSummaryProvider.instance
			it.setItems(createItems())
			it.value = binder.get()
			it.setOnPreferenceChangeListener { _, newValue ->
				binder.set(newValue as UserSelection)
				it.value = binder.get()
				container()

				// Always return false because we save it
				false
			}
		}

		container += {
			pref.isEnabled = dependencyCheckFun() && enabled
		}
	}

	data class UserSelection(
		val behavior: UserSelectBehavior,
		val userId: UUID?
	)
}

@OptionsDSL
fun OptionsCategory.userPicker(
	authenticationRepository: AuthenticationRepository,
	init: OptionsItemUserPicker.() -> Unit
) {
	this += OptionsItemUserPicker(context, authenticationRepository).apply { init() }
}
