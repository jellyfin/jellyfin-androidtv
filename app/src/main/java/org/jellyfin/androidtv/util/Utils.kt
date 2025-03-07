package org.jellyfin.androidtv.util

import android.content.Context
import android.widget.Toast
import org.jellyfin.sdk.model.api.UserDto
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.core.component.KoinComponent
import java.util.UUID
import kotlin.math.roundToInt

/**
 * A collection of utility methods, all static.
 */
object Utils : KoinComponent {
	/**
	 * Shows a (long) toast
	 */
	@JvmStatic
	@Deprecated(
		message = "Use Toast.makeText",
		replaceWith = ReplaceWith(
			expression = "Toast.makeText(context, msg, Toast.LENGTH_LONG).show()",
			imports = ["android.widget.Toast"]
		)
	)
	fun showToast(context: Context?, msg: String?) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
	}

	/**
	 * Shows a (long) toast.
	 */
	@JvmStatic
	@Deprecated(
		message = "Use Toast.makeText",
		replaceWith = ReplaceWith(
			expression = "Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show()",
			imports = ["android.widget.Toast"]
		)
	)
	fun showToast(context: Context, resourceId: Int) {
		Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show()
	}

	@JvmStatic
	fun convertDpToPixel(ctx: Context, dp: Int): Int = (dp * ctx.resources.displayMetrics.density).roundToInt()

	@JvmStatic
	fun isTrue(value: Boolean?): Boolean = value == true

	@JvmStatic
	fun <T> getSafeValue(value: T?, defaultValue: T): T = value ?: defaultValue

	@JvmStatic
	fun isEmpty(value: String?): Boolean = value.isNullOrEmpty()

	@JvmStatic
	fun getThemeColor(context: Context, resourceId: Int): Int {
		val styledAttributes = context.theme.obtainStyledAttributes(intArrayOf(resourceId))
		val themeColor = styledAttributes.getColor(0, 0)
		styledAttributes.recycle()
		return themeColor
	}

	@JvmStatic
	fun getSafeSeekPosition(position: Long, duration: Long): Long = when {
		position >= duration -> (duration - 1000).coerceAtLeast(0)
		else -> position.coerceAtLeast(0)
	}

	@JvmStatic
	fun canManageRecordings(user: UserDto?): Boolean = user?.policy?.enableLiveTvManagement == true

	@JvmStatic
	fun uuidOrNull(string: String?): UUID? = string?.toUUIDOrNull()
}
