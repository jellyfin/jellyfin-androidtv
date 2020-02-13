package org.jellyfin.androidtv.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.BaseCardView
import kotlinx.android.synthetic.main.multi_badge_image_card_view.view.*
import org.jellyfin.androidtv.R

class MultiBadgeImageCardView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : BaseCardView(context, attrs, defStyleAttr) {
	private val mFadeInAnimator: ObjectAnimator
	private var mAttachedToWindow: Boolean = false

	var mainImageDrawable: Drawable?
		get() = main_image.drawable
		set(value) { setMainImage(value, true) }

	var titleText: CharSequence
		get() = title_text.text
		set(value) { title_text.text = value }

	var contentText: CharSequence?
		get() = content_text.text
		set(value) { content_text.text = value }

	val badgeContainers: Map<BadgeLocation, ViewGroup>

	constructor(context: Context) : this(context, null)
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.imageCardViewStyle)

	init {
	    isFocusable = true
		isFocusableInTouchMode = true

		val inflater = LayoutInflater.from(context)
		inflater.inflate(R.layout.multi_badge_image_card_view, this)
		val cardAttrs = context.obtainStyledAttributes(attrs, R.styleable.lbImageCardView, defStyleAttr, R.style.Widget_Leanback_ImageCardView)

		// TODO ViewCompat.saveAttributeDataForStyleable call that is here in the original


		mFadeInAnimator = ObjectAnimator.ofFloat(main_image, View.ALPHA, 1f).apply {
			duration = main_image.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
		}

		val backgroundDrawable: Drawable? = cardAttrs.getDrawable(R.styleable.lbImageCardView_infoAreaBackground)
		if (backgroundDrawable != null) {
			background = backgroundDrawable
		}

		badgeContainers = mapOf<BadgeLocation, ViewGroup>(
			BadgeLocation.TOP_LEFT to badge_top_left,
			BadgeLocation.TOP_RIGHT to badge_top_right,
			BadgeLocation.BOTTOM_LEFT to badge_bottom_left,
			BadgeLocation.BOTTOM_RIGHT to badge_bottom_right,
			BadgeLocation.CONTENT_LEFT to content_badge_left,
			BadgeLocation.CONTENT_RIGHT to content_badge_right
		)

		cardAttrs.recycle()
	}

	fun setBadge(badgeLocation: BadgeLocation, badge: View?) {
		val badgeContainer = badgeContainers.getValue(badgeLocation)
		badgeContainer.removeAllViews()

		if (badge != null) {
			badgeContainer.apply {
				visibility = ViewGroup.VISIBLE
				addView(badge)
			}
		} else {
			badgeContainer.apply {
				visibility = ViewGroup.GONE
			}
		}
	}

	fun setMainImage(drawable: Drawable?, fade: Boolean) {
		main_image.setImageDrawable(drawable)

		if (drawable == null) {
			mFadeInAnimator.cancel()
			main_image.apply {
				alpha = 1f
				visibility = View.INVISIBLE
			}
		} else {
			if (fade) {
				fadeIn()
			} else {
				mFadeInAnimator.cancel()
				main_image.alpha = 1f
			}
		}
	}

	fun setMainImageDimensions(width: Int, height: Int) {
		val params = main_image.layoutParams.apply {
			this.width = width
			this.height = height
		}
		main_image.layoutParams = params
	}

	private fun fadeIn() {
		main_image.alpha = 0f
		if (mAttachedToWindow) {
			mFadeInAnimator.start()
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		mAttachedToWindow = true
		if (main_image.alpha == 0f)
			fadeIn()
	}

	override fun onDetachedFromWindow() {
		mAttachedToWindow = false
		mFadeInAnimator.cancel()
		main_image.alpha = 1f
		super.onDetachedFromWindow()
	}

	enum class BadgeLocation {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CONTENT_LEFT, CONTENT_RIGHT
	}
}
