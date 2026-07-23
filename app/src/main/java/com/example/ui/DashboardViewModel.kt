package com.example.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BlockerPreferences
import com.example.data.BlockerSettings
import com.example.service.FocusAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val prefs: BlockerPreferences) : ViewModel() {

    val settings: StateFlow<BlockerSettings> = prefs.settings

    private val _isAccessibilityGranted = MutableStateFlow(false)
    val isAccessibilityGranted: StateFlow<Boolean> = _isAccessibilityGranted.asStateFlow()

    fun checkAccessibilityPermission(context: Context) {
        viewModelScope.launch {
            _isAccessibilityGranted.value = isAccessibilityServiceEnabled(context)
        }
    }

    fun toggleMasterSwitch(enabled: Boolean) {
        prefs.setMasterEnabled(enabled)
    }

    fun toggleRedirectSubscriptions(enabled: Boolean) {
        prefs.setRedirectSubscriptions(enabled)
    }

    fun toggleBlockShorts(enabled: Boolean) {
        prefs.setBlockShorts(enabled)
    }

    fun resetStats() {
        prefs.resetStats()
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, FocusAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    class Factory(private val prefs: BlockerPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(prefs) as T
        }
    }
}
