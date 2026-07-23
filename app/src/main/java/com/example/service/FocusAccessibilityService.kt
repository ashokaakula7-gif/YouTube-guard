package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.BlockerPreferences

class FocusAccessibilityService : AccessibilityService() {

    private lateinit var prefs: BlockerPreferences

    private var wasInYouTube = false
    private var lastRedirectTime: Long = 0
    private var lastShortsBlockTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        prefs = BlockerPreferences.getInstance(applicationContext)
        Log.d(TAG, "FocusAccessibilityService created")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val settings = prefs.settings.value
        if (!settings.isMasterEnabled) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        if (packageName != YOUTUBE_PACKAGE) {
            wasInYouTube = false
            return
        }

        val currentTime = SystemClock.elapsedRealtime()

        // 1. Direct-to-Subscriptions Redirect
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val isNewSession = !wasInYouTube || (currentTime - lastRedirectTime > LAUNCH_COOLDOWN_MS)
            wasInYouTube = true

            if (settings.redirectSubscriptions && isNewSession) {
                val className = event.className?.toString() ?: ""
                // Don't loop redirect if we are already opening subscriptions
                if (!className.contains("Subscriptions", ignoreCase = true)) {
                    lastRedirectTime = currentTime
                    redirectToSubscriptions()
                }
            }
        }

        // 2. YouTube Shorts Blocking
        if (settings.blockShorts) {
            val className = event.className?.toString() ?: ""
            val isShortsActivity = className.contains("Reel", ignoreCase = true) ||
                    className.contains("Shorts", ignoreCase = true)

            val rootNode = rootInActiveWindow
            val isShortsDetected = isShortsActivity || (rootNode != null && containsShortsElement(rootNode))

            if (isShortsDetected && (currentTime - lastShortsBlockTime > SHORTS_COOLDOWN_MS)) {
                lastShortsBlockTime = currentTime
                blockShorts()
            }
        }
    }

    private fun redirectToSubscriptions() {
        Log.d(TAG, "Redirecting YouTube directly to Subscriptions feed")
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("youtube://www.youtube.com/feed/subscriptions")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                setPackage(YOUTUBE_PACKAGE)
            }
            startActivity(appIntent)
        } catch (e: Exception) {
            // Fallback to web link if URI scheme fails
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/feed/subscriptions")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to launch YouTube subscriptions", ex)
            }
        }
        prefs.incrementRedirectCount()
    }

    private fun blockShorts() {
        Log.d(TAG, "YouTube Shorts detected! Executing action to exit Shorts")
        // Attempt global BACK action to exit Shorts
        val backSuccess = performGlobalAction(GLOBAL_ACTION_BACK)
        if (!backSuccess) {
            // If BACK action fails, re-redirect to Subscriptions
            redirectToSubscriptions()
        }
        prefs.incrementShortsBlockedCount()
    }

    private fun containsShortsElement(node: AccessibilityNodeInfo, depth: Int = 0): Boolean {
        if (depth > MAX_SEARCH_DEPTH) return false

        val viewId = node.viewIdResourceName
        if (viewId != null) {
            val lowerId = viewId.lowercase()
            if (lowerId.contains("reel_") || lowerId.contains("shorts_") ||
                lowerId.contains("reel_player") || lowerId.contains("shorts_container")
            ) {
                return true
            }
        }

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()

        if ((text != null && SHORTS_KEYWORDS.any { text.equals(it, ignoreCase = true) }) ||
            (desc != null && SHORTS_KEYWORDS.any { desc.equals(it, ignoreCase = true) })
        ) {
            // Check if it's a Shorts player view context
            if (node.className?.toString()?.contains("Reel", ignoreCase = true) == true ||
                node.className?.toString()?.contains("Shorts", ignoreCase = true) == true
            ) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsShortsElement(child, depth + 1)) {
                return true
            }
        }

        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "FocusAccessibilityService interrupted")
    }

    companion object {
        private const val TAG = "FocusService"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val LAUNCH_COOLDOWN_MS = 8000L
        private const val SHORTS_COOLDOWN_MS = 1500L
        private const val MAX_SEARCH_DEPTH = 15

        private val SHORTS_KEYWORDS = listOf("Shorts", "Shorts player", "Shorts video", "Reel")
    }
}
