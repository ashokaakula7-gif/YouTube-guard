package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BlockerSettings(
    val isMasterEnabled: Boolean = true,
    val redirectSubscriptions: Boolean = true,
    val blockShorts: Boolean = true,
    val redirectCount: Int = 0,
    val shortsBlockedCount: Int = 0
)

class BlockerPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<BlockerSettings> = _settings.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = loadSettings()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun loadSettings(): BlockerSettings {
        return BlockerSettings(
            isMasterEnabled = prefs.getBoolean(KEY_MASTER_ENABLED, true),
            redirectSubscriptions = prefs.getBoolean(KEY_REDIRECT_SUBSCRIPTIONS, true),
            blockShorts = prefs.getBoolean(KEY_BLOCK_SHORTS, true),
            redirectCount = prefs.getInt(KEY_REDIRECT_COUNT, 0),
            shortsBlockedCount = prefs.getInt(KEY_SHORTS_BLOCKED_COUNT, 0)
        )
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        _settings.value = loadSettings()
    }

    fun setRedirectSubscriptions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDIRECT_SUBSCRIPTIONS, enabled).apply()
        _settings.value = loadSettings()
    }

    fun setBlockShorts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_SHORTS, enabled).apply()
        _settings.value = loadSettings()
    }

    fun incrementRedirectCount() {
        val current = prefs.getInt(KEY_REDIRECT_COUNT, 0)
        prefs.edit().putInt(KEY_REDIRECT_COUNT, current + 1).apply()
        _settings.value = loadSettings()
    }

    fun incrementShortsBlockedCount() {
        val current = prefs.getInt(KEY_SHORTS_BLOCKED_COUNT, 0)
        prefs.edit().putInt(KEY_SHORTS_BLOCKED_COUNT, current + 1).apply()
        _settings.value = loadSettings()
    }

    fun resetStats() {
        prefs.edit()
            .putInt(KEY_REDIRECT_COUNT, 0)
            .putInt(KEY_SHORTS_BLOCKED_COUNT, 0)
            .apply()
        _settings.value = loadSettings()
    }

    companion object {
        private const val PREFS_NAME = "yt_focus_prefs"
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_REDIRECT_SUBSCRIPTIONS = "redirect_subscriptions"
        private const val KEY_BLOCK_SHORTS = "block_shorts"
        private const val KEY_REDIRECT_COUNT = "redirect_count"
        private const val KEY_SHORTS_BLOCKED_COUNT = "shorts_blocked_count"

        @Volatile
        private var INSTANCE: BlockerPreferences? = null

        fun getInstance(context: Context): BlockerPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlockerPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
