package com.example.suslog.settings

import android.content.Context

class AppSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "suslog_settings",
        Context.MODE_PRIVATE
    )

    fun isAppLockEnabled(): Boolean =
        preferences.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            .apply()
    }

    private companion object {
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    }
}
