package org.jellyfin.androidtv.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog

class StillWatchingDialogFragment : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return AlertDialog.Builder(requireContext())
			.setTitle("Êtes-vous toujours en train de regarder?")
			.setMessage("Il semble que vous avez regardé pendant un certain temps. Voulez-vous continuer?")
			.setPositiveButton("Oui") { dialog, _ ->
				dialog.dismiss()
				(activity as? WatchTrackerViewModel)?.onPromptDismissed()
			}
			.setNegativeButton("Non") { dialog, _ ->
				dialog.dismiss()
				// Ajoutez ici toute action à effectuer si l'utilisateur choisit "Non"
			}
			.create()
	}
}
