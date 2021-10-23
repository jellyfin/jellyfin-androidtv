package org.jellyfin.androidtv.ui.itempreview

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.jellyfin.androidtv.databinding.FragmentItemPreviewBinding
import org.jellyfin.apiclient.model.dto.BaseItemType
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class ItemPreviewFragment : Fragment() {
	private val viewModel: ItemPreviewViewModel by sharedViewModel()
	private lateinit var binding: FragmentItemPreviewBinding

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		binding = FragmentItemPreviewBinding.inflate(inflater, container, false)

		viewModel.item.observe(viewLifecycleOwner) { data ->
			// No data, keep current
			if (data == null) return@observe

			binding.root.isVisible = true
			binding.title.text = data.title
			binding.numbersRow.text = data.numbersString
			binding.subtitle.text = data.subtitle
			binding.homeRowHeader.text = data.rowHeader

			when {
				data.baseItem.baseItemType == BaseItemType.Episode
					|| data.baseItem.baseItemType == BaseItemType.MusicAlbum
					|| !data.baseItem.taglines.isNullOrEmpty() -> binding.subtitle.setTextSize(
					TypedValue.COMPLEX_UNIT_SP,
					16F
				)
				else -> binding.subtitle.setTextSize(
					TypedValue.COMPLEX_UNIT_SP,
					15F
				)
			}

			if (data.logoImageUrl != null) {
				binding.logo.isVisible = true
				binding.title.isVisible = false
				Glide.with(requireContext())
					.load(data.logoImageUrl)
					.listener(object : RequestListener<Drawable> {
						override fun onResourceReady(
							resource: Drawable?,
							model: Any?,
							target: Target<Drawable>?,
							dataSource: DataSource?,
							isFirstResource: Boolean
						): Boolean {
							binding.logo.contentDescription = data.baseItem.name
							return false
						}

						override fun onLoadFailed(
							e: GlideException?,
							model: Any?,
							target: Target<Drawable>?,
							isFirstResource: Boolean
						): Boolean {
							binding.logo.isVisible = false
							binding.title.isVisible = true
							return false
						}
					})
					.override(Target.SIZE_ORIGINAL)
					.into(binding.logo)
			} else {
				binding.logo.isVisible = false
				binding.title.isVisible = true
			}
		}
		return binding.root
	}
}
