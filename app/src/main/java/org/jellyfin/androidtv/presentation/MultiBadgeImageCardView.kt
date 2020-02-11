package org.jellyfin.androidtv.presentation

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.leanback.widget.BaseCardView
import kotlinx.android.synthetic.main.multi_badge_image_card_view.view.*
import org.jellyfin.androidtv.R

class MultiBadgeImageCardView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : BaseCardView(context, attrs, defStyleAttr) {
	private val mFadeInAnimator: ObjectAnimator
	private var mAttachedToWindow: Boolean = false

	var mainImageDrawable: Drawable?
		get() = mainImage.drawable
		set(value) { setMainImage(value, true) }

	var titleText: CharSequence
		get() = title_text.text
		set(value) { title_text.text = value }

	var contentText: CharSequence?
		get() = content_text.text
		set(value) { content_text.text = value }

	constructor(context: Context) : this(context, null)
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.imageCardViewStyle)

	init {
	    isFocusable = true
		isFocusableInTouchMode = true

		val inflater = LayoutInflater.from(context)
		inflater.inflate(R.layout.multi_badge_image_card_view, this)
		val cardAttrs = context.obtainStyledAttributes(attrs, R.styleable.lbImageCardView, defStyleAttr, R.style.Widget_Leanback_ImageCardView)

		// TODO ViewCompat.saveAttributeDataForStyleable call that is here in the original


		mFadeInAnimator = ObjectAnimator.ofFloat(mainImage, View.ALPHA, 1f).apply {
			duration = mainImage.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
		}

		val backgroundDrawable: Drawable? = cardAttrs.getDrawable(R.styleable.lbImageCardView_infoAreaBackground)
		if (backgroundDrawable != null) {
			background = backgroundDrawable
		}

		cardAttrs.recycle()
	}



	fun setMainImage(drawable: Drawable?, fade: Boolean) {
		mainImage.setImageDrawable(drawable)

		if (drawable == null) {
			mFadeInAnimator.cancel()
			mainImage.apply {
				alpha = 1f
				visibility = View.INVISIBLE
			}
		} else {
			if (fade) {
				fadeIn()
			} else {
				mFadeInAnimator.cancel()
				mainImage.alpha = 1f
			}
		}
	}

	fun setMainImageDimensions(width: Int, height: Int) {
		val params = mainImage.layoutParams.apply {
			this.width = width
			this.height = height
		}
		mainImage.layoutParams = params
	}

	private fun fadeIn() {
		mainImage.alpha = 0f
		if (mAttachedToWindow) {
			mFadeInAnimator.start()
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		mAttachedToWindow = true
		if (mainImage.alpha == 0f)
			fadeIn()
	}

	override fun onDetachedFromWindow() {
		mAttachedToWindow = false
		mFadeInAnimator.cancel()
		mainImage.alpha = 1f
		super.onDetachedFromWindow()
	}
}
