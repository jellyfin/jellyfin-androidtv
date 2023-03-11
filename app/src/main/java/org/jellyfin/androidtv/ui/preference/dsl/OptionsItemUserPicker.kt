package org.jellyfin.androidtv.ui.preference.dsl

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceCategory
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem.RichListOption
import org.jellyfin.androidtv.ui.preference.custom.RichListDialogFragment.RichListItem.RichListSection
import org.jellyfin.androidtv.ui.preference.custom.RichListPreference
import java.text.DateFormat
import java.util.UUID

class OptionsItemUserPicker(
	private val context: Context,
	private val serverRepository: ServerRepository,
	private val serverUserRepository: ServerUserRepository,
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
		serverId: UUID? = null,
		userId: UUID? = null,
		title: String,
		summary: String
	) = add(
		RichListOption(
			UserSelection(
				behavior = behavior,
				serverId = serverId,
				userId = userId,
			),
			title,
			summary
		)
	)

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
		for (server in serverRepository.storedServers.value) {
			val users = serverUserRepository.getStoredServerUsers(server)
			if (users.isEmpty()) continue

			add(RichListSection(server.name))

			for (user in users) add(
				behavior = UserSelectBehavior.SPECIFIC_USER,
				serverId = user.serverId,
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
		val serverId: UUID?,
		val userId: UUID?,
	)
}

@OptionsDSL
fun OptionsCategory.userPicker(
	serverRepository: ServerRepository,
	serverUserRepository: ServerUserRepository,
	init: OptionsItemUserPicker.() -> Unit
) {
	this += OptionsItemUserPicker(context, serverRepository, serverUserRepository).apply { init() }
}
