package org.jellyfin.androidtv.util

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserPreferences.Companion.audioBehaviour
import org.jellyfin.androidtv.preference.constant.AudioBehavior
import org.jellyfin.sdk.model.api.UserDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber
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

	/**
	 * A null safe version of `String.equalsIgnoreCase`.
	 */
	@JvmStatic
	fun equalsIgnoreCase(str1: String?, str2: String?): Boolean = when {
		str1 == null && str2 == null -> true
		str1 == null || str2 == null -> false
		else -> str1.equals(str2, ignoreCase = true)
	}

	@JvmStatic
	fun <T> getSafeValue(value: T?, defaultValue: T): T = value ?: defaultValue

	@JvmStatic
	fun isEmpty(value: String?): Boolean = value.isNullOrEmpty()

	@JvmStatic
	fun isNonEmpty(value: String?): Boolean = !value.isNullOrEmpty()

	@JvmStatic
	fun join(separator: String, items: Iterable<String?>): String = items.joinToString(separator = separator)

	@JvmStatic
	fun join(separator: String, vararg items: String?): String = join(separator, items.toList())

	@JvmStatic
	fun getMaxBitrate(userPreferences: UserPreferences): Int {
		var maxRate = userPreferences[UserPreferences.maxBitrate]

		// Use default when value is what was previously "auto"
		if (maxRate == "0") maxRate = UserPreferences.maxBitrate.defaultValue

		// Convert megabit to bit
		return (maxRate.toFloat() * 1_000_000).toInt()
	}

	@JvmStatic
	fun getThemeColor(context: Context, resourceId: Int): Int {
		val styledAttributes = context.theme.obtainStyledAttributes(intArrayOf(resourceId))
		val themeColor = styledAttributes.getColor(0, 0)
		styledAttributes.recycle()
		return themeColor
	}

	@JvmStatic
	fun downMixAudio(context: Context): Boolean {
		val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
		if (am.isBluetoothA2dpOn) {
			Timber.i("Downmixing audio due to wired headset")
			return true
		}

		return get<UserPreferences>()[audioBehaviour] === AudioBehavior.DOWNMIX_TO_STEREO
	}

	@JvmStatic
	fun getSafeSeekPosition(position: Long, duration: Long): Long = when {
		position >= duration -> (duration - 1000).coerceAtLeast(0)
		else -> position.coerceAtLeast(0)
	}

	@JvmStatic
	fun canManageRecordings(user: UserDto?): Boolean = user?.policy?.enableLiveTvManagement == true
}
