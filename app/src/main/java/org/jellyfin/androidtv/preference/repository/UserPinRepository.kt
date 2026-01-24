package org.jellyfin.androidtv.preference.repository

import android.content.Context
import androidx.preference.PreferenceManager
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference
import java.security.MessageDigest
import java.util.UUID

class UserPinRepository(context: Context) : SharedPreferenceStore(
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {
    private fun getPinPreference(userId: UUID) = stringPreference("user_pin_$userId", "")

    fun setPin(userId: UUID, pin: String) {
        val hash = hashPin(pin)
        this[getPinPreference(userId)] = hash
    }

    fun removePin(userId: UUID) {
        delete(getPinPreference(userId))
    }

    fun hasPin(userId: UUID): Boolean {
        return sharedPreferences.contains("user_pin_$userId")
    }

    fun verifyPin(userId: UUID, pin: String): Boolean {
        val storedHash = this[getPinPreference(userId)]
        if (storedHash.isEmpty()) return false
        return storedHash == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
