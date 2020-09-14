package org.jellyfin.androidtv.ui

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityOptionsCompat
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.TvApp
import org.jellyfin.androidtv.ui.itemdetail.ExpandedTextActivity
import org.jellyfin.androidtv.util.getActivity

class ExpandableTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
	init {
		background = context.getDrawable(R.drawable.expanded_text)
		transitionName = ExpandedTextActivity.TRANSITION_NAME

		setOnClickListener {
			val activity = context.getActivity() ?: return@setOnClickListener

			val intent = Intent(TvApp.getApplication(), ExpandedTextActivity::class.java).apply {
				putExtra(ExpandedTextActivity.EXTRA_TEXT, text.toString())
			}

			val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
				activity,
				this,
				ExpandedTextActivity.TRANSITION_NAME
			)

			activity.startActivity(intent, options.toBundle())
		}
	}
}
