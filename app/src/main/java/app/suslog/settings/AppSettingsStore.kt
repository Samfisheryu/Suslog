package app.suslog.settings

import android.content.Context
import app.suslog.domain.suspension.StiffSide
import app.suslog.domain.suspension.SuspensionType

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

    fun isDefaultAxleLockEnabled(): Boolean =
        preferences.getBoolean(KEY_DEFAULT_AXLE_LOCK_ENABLED, true)

    fun setDefaultAxleLockEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_DEFAULT_AXLE_LOCK_ENABLED, enabled)
            .apply()
    }

    fun defaultSuspensionType(): SuspensionType =
        preferences.getString(KEY_DEFAULT_SUSPENSION_TYPE, null)
            ?.let { runCatching { SuspensionType.valueOf(it) }.getOrNull() }
            ?: SuspensionType.TWO_WAY

    fun setDefaultSuspensionType(type: SuspensionType) {
        preferences.edit()
            .putString(KEY_DEFAULT_SUSPENSION_TYPE, type.name)
            .apply()
    }

    fun defaultMaxClicks(): Int =
        preferences.getInt(KEY_DEFAULT_MAX_CLICKS, 30).coerceIn(1, 99)

    fun setDefaultMaxClicks(maxClicks: Int) {
        preferences.edit()
            .putInt(KEY_DEFAULT_MAX_CLICKS, maxClicks.coerceIn(1, 99))
            .apply()
    }

    fun defaultStiffSide(): StiffSide =
        preferences.getString(KEY_DEFAULT_STIFF_SIDE, null)
            ?.let { runCatching { StiffSide.valueOf(it) }.getOrNull() }
            ?: StiffSide.HIGH_VALUE

    fun setDefaultStiffSide(stiffSide: StiffSide) {
        preferences.edit()
            .putString(KEY_DEFAULT_STIFF_SIDE, stiffSide.name)
            .apply()
    }

    fun requireFeedbackBeforeExitConfig(): Boolean =
        preferences.getBoolean(KEY_REQUIRE_FEEDBACK_BEFORE_EXIT_CONFIG, true)

    fun setRequireFeedbackBeforeExitConfig(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_REQUIRE_FEEDBACK_BEFORE_EXIT_CONFIG, enabled)
            .apply()
    }

    fun showPreviousFeedbackReference(): Boolean =
        preferences.getBoolean(KEY_SHOW_PREVIOUS_FEEDBACK_REFERENCE, true)

    fun setShowPreviousFeedbackReference(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SHOW_PREVIOUS_FEEDBACK_REFERENCE, enabled)
            .apply()
    }

    fun showSetupDebugInfo(): Boolean =
        preferences.getBoolean(KEY_SHOW_SETUP_DEBUG_INFO, false)

    fun setShowSetupDebugInfo(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SHOW_SETUP_DEBUG_INFO, enabled)
            .apply()
    }

    private companion object {
        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_DEFAULT_AXLE_LOCK_ENABLED = "default_axle_lock_enabled"
        const val KEY_DEFAULT_SUSPENSION_TYPE = "default_suspension_type"
        const val KEY_DEFAULT_MAX_CLICKS = "default_max_clicks"
        const val KEY_DEFAULT_STIFF_SIDE = "default_stiff_side"
        const val KEY_REQUIRE_FEEDBACK_BEFORE_EXIT_CONFIG = "require_feedback_before_exit_config"
        const val KEY_SHOW_PREVIOUS_FEEDBACK_REFERENCE = "show_previous_feedback_reference"
        const val KEY_SHOW_SETUP_DEBUG_INFO = "show_setup_debug_info"
    }
}

data class SetupDefaults(
    val axleLockEnabled: Boolean,
    val suspensionType: SuspensionType,
    val maxClicks: Int,
    val stiffSide: StiffSide,
)

data class TuningPreferences(
    val requireFeedbackBeforeExitConfig: Boolean,
    val showPreviousFeedbackReference: Boolean,
    val showSetupDebugInfo: Boolean,
)
