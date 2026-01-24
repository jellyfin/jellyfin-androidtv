package org.jellyfin.androidtv.preference.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class UserPinRepositoryTest {

    private val context = mockk<Context>()
    private val sharedPreferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    private lateinit var repository: UserPinRepository

    @BeforeEach
    fun setup() {
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(context) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        repository = UserPinRepository(context)
    }

    @Test
    fun `setPin stores hashed pin`() {
        val userId = UUID.randomUUID()
        val pin = "1234"
        val key = "user_pin_$userId"
        val slotKey = slot<String>()
        val slotValue = slot<String>()

        every { editor.putString(capture(slotKey), capture(slotValue)) } returns editor

        repository.setPin(userId, pin)

        verify { editor.putString(key, any()) }
        assertTrue(slotValue.captured.isNotEmpty())
        assertFalse(slotValue.captured == pin) // Should be hashed
    }

    @Test
    fun `verifyPin returns true for correct pin`() {
        val userId = UUID.randomUUID()
        val pin = "1234"
        val key = "user_pin_$userId"
        
        // We need to capture what setPin stores to verify it against verifyPin logic which re-hashes
        // Or we just test that verifyPin uses getString and hashes input.
        
        // Let's implement setPin effectively in the mock
        val storedHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4" // SHA-256 for 1234
        every { sharedPreferences.getString(key, "") } returns storedHash

        assertTrue(repository.verifyPin(userId, pin))
    }

    @Test
    fun `verifyPin returns false for incorrect pin`() {
        val userId = UUID.randomUUID()
        val pin = "1234"
        val key = "user_pin_$userId"
        
        val storedHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4" // SHA-256 for 1234
        every { sharedPreferences.getString(key, "") } returns storedHash

        assertFalse(repository.verifyPin(userId, "0000"))
    }
}
