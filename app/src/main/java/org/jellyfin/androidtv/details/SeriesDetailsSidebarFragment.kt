package org.jellyfin.androidtv.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.jellyfin.androidtv.databinding.SeriesDetailsSidebarBinding
import org.jellyfin.androidtv.model.itemtypes.BaseItem

class SeriesDetailsSidebarFragment(private val item: BaseItem) : Fragment() {
	private lateinit var binding: SeriesDetailsSidebarBinding

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		binding = SeriesDetailsSidebarBinding.inflate(inflater, container, false)

		binding.title.text = item.title
		binding.description.text = item.description

		return binding.root
	}
}
