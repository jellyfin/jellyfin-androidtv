package org.jellyfin.androidtv.ui.itemhandling

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.data.model.ChapterItemInfo
import org.jellyfin.androidtv.ui.GridButton
import org.jellyfin.androidtv.util.ImageHelper
import org.jellyfin.androidtv.util.apiclient.LifecycleAwareResponse
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.SeriesTimerInfoDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

// TODO: Move properties to relevant classes only (e.g. baseItem should be in
//  BaseItemDtoBaseRowItem)
abstract class BaseRowItem protected constructor(
	val baseRowType: BaseRowType,
	var index: Int = 0,
	val staticHeight: Boolean = false,
	val preferParentThumb: Boolean = false,
	val selectAction: BaseRowItemSelectAction = BaseRowItemSelectAction.ShowDetails,
	var baseItem: BaseItemDto? = null,
	val chapterInfo: ChapterItemInfo? = null,
	val seriesTimerInfo: SeriesTimerInfoDto? = null,
	val gridButton: GridButton? = null,
) : KoinComponent {
	val imageHelper by inject<ImageHelper>()

	open val showCardInfoOverlay: Boolean = false
	open fun getBaseItemType(): BaseItemKind? = null
	open fun isFavorite(): Boolean = false
	open fun isPlayed(): Boolean = false

	open fun getCardName(context: Context): String? = getFullName(context)

	open fun getChildCountStr(): String? = null

	open fun getImageUrl(
		context: Context,
		imageType: ImageType,
		fillWidth: Int,
		fillHeight: Int,
	) = getPrimaryImageUrl(context, fillHeight)

	open fun getPrimaryImageUrl(context: Context, fillHeight: Int): String? = null
	open fun getFullName(context: Context): String? = null
	open fun getName(context: Context): String? = null
	open fun getItemId(): UUID? = null
	open fun getSubText(context: Context): String? = null
	open fun getSummary(context: Context): String? = null
	open fun getBadgeImage(context: Context): Drawable? = null

	@JvmOverloads
	open fun refresh(
		outerResponse: LifecycleAwareResponse<BaseItemDto?>,
		scope: CoroutineScope = ProcessLifecycleOwner.get().lifecycleScope,
	) = Unit

	override fun equals(other: Any?): Boolean {
		if (other is BaseRowItem) return other.getItemId() == getItemId()
		return super.equals(other)
	}
}
