package com.example.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gkk_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_APP_PIN = "app_pin"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCK_TIMESTAMP = "lock_timestamp"
        private const val KEY_REPEAT_FAILURES = "repeat_failures"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_DEVELOPER_MODE = "developer_mode"
        private const val KEY_DEVELOPER_PASSWORD = "developer_password"
        private const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        
        private const val DEFAULT_PIN = "451733"
        private const val MASTER_PIN = "904945"
        private const val OWNER_NAME = "Eknath Khelwade"
        
        private const val MAX_ATTEMPTS = 5
        private const val REPEAT_LOCK_THRESHOLD = 10
        private const val LOCK_TIME_SHORT = 30_000L // 30 seconds
        private const val LOCK_TIME_LONG = 300_000L // 5 minutes
    }

    fun getPin(): String {
        return sharedPreferences.getString(KEY_APP_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    fun setPin(newPin: String): Boolean {
        if (newPin.length != 6 || isSimplePin(newPin)) return false
        sharedPreferences.edit().putString(KEY_APP_PIN, newPin).apply()
        return true
    }

    private fun isSimplePin(pin: String): Boolean {
        val simplePins = listOf("000000", "111111", "222222", "333333", "444444", "555555", "666666", "777777", "888888", "999999", "123456", "654321")
        return pin in simplePins
    }

    fun validatePin(enteredPin: String): Boolean {
        if (getRemainingLockTime() > 0) return false

        val isValid = enteredPin == getPin() || enteredPin == MASTER_PIN
        
        if (isValid) {
            resetFailedAttempts()
        } else {
            incrementFailedAttempts()
        }
        
        return isValid
    }

    fun getRemainingLockTime(): Long {
        val lockTimestamp = sharedPreferences.getLong(KEY_LOCK_TIMESTAMP, 0L)
        if (lockTimestamp == 0L) return 0L
        
        val currentAttempts = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0)
        val lockDuration = if (currentAttempts >= REPEAT_LOCK_THRESHOLD) LOCK_TIME_LONG else LOCK_TIME_SHORT
        
        val elapsed = System.currentTimeMillis() - lockTimestamp
        val remaining = lockDuration - elapsed
        
        return if (remaining > 0) remaining else 0L
    }

    private fun incrementFailedAttempts() {
        val currentAttempts = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_FAILED_ATTEMPTS, currentAttempts)
        
        if (currentAttempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_LOCK_TIMESTAMP, System.currentTimeMillis())
        }
        editor.apply()
    }

    private fun resetFailedAttempts() {
        sharedPreferences.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCK_TIMESTAMP, 0L)
            .apply()
    }

    fun validateOwnerName(name: String): Boolean {
        val isValid = name.equals(OWNER_NAME, ignoreCase = true)
        if (isValid) resetFailedAttempts()
        return isValid
    }

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isDeveloperModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_DEVELOPER_MODE, false)
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply()
    }

    fun setDeveloperPassword(password: String) {
        // In a real app, we'd hash this. But since it's in EncryptedSharedPreferences, it's already encrypted on disk.
        sharedPreferences.edit().putString(KEY_DEVELOPER_PASSWORD, password).apply()
    }

    fun validateDeveloperPassword(password: String): Boolean {
        val stored = sharedPreferences.getString(KEY_DEVELOPER_PASSWORD, null) ?: "271981"
        return password == stored
    }

    fun isCloudSyncEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_CLOUD_SYNC_ENABLED, enabled).apply()
    }
}
