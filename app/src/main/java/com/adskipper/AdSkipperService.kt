package com.adskipper

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdSkipperService : AccessibilityService() {

    companion object {
        private const val TAG = "AdSkipperService"

        /**
         * Patterns that unambiguously identify an ad skip button.
         * Clicked immediately without requiring further confirmation.
         */
        val AD_SKIP_PATTERNS = listOf(
            "skip_ad_button",
            "ad_skip_button",
            "Skip Ad",
            "Skip ad",
            "Skip Ads"
        )

        /**
         * Patterns that could also appear in normal video playback controls
         * (e.g. the "skip forward" or "next video" buttons). Only acted on
         * when an ad indicator is also visible in the view hierarchy.
         */
        val AMBIGUOUS_SKIP_PATTERNS = listOf(
            "skip_button",
            "Skip"
        )

        /**
         * View ID fragments that only appear in YouTube's ad overlay.
         * Matched via viewId.contains() — keep these specific enough to avoid
         * false matches (e.g. don't use bare "ad", which hits "loaded", "upload", etc.)
         */
        val AD_INDICATOR_VIEW_IDS = listOf(
            "ad_badge",
            "ad_progress",
            "ad_text",
            "ad_counter",
            "ad_duration"
        )

        /**
         * Exact text/content-description labels shown only during ads.
         * Matched via equals() only — never used in a contains() check.
         */
        val AD_INDICATOR_LABELS = listOf(
            "Ad",
            "Sponsored"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        // Check the active window
        rootInActiveWindow?.let { root ->
            try {
                findAndClick(root, hasAdIndicator(root))
            } finally {
                root.recycle()
            }
        }

        // Also scan PiP windows — requires API 26 and flagRetrieveInteractiveWindows
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            windows
                .filter { it.isInPictureInPictureMode }
                .forEach { window ->
                    val pipRoot = window.root ?: return@forEach
                    try {
                        findAndClick(pipRoot, hasAdIndicator(pipRoot))
                    } finally {
                        pipRoot.recycle()
                    }
                }
        }
    }

    /**
     * Returns true if any node in the hierarchy signals that an ad is playing.
     */
    private fun hasAdIndicator(node: AccessibilityNodeInfo): Boolean {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val desc   = node.contentDescription?.toString() ?: ""
        val text   = node.text?.toString() ?: ""

        val matchedViewId = AD_INDICATOR_VIEW_IDS.any { pattern -> viewId.contains(pattern.lowercase()) }
        val matchedLabel  = AD_INDICATOR_LABELS.any { pattern ->
            desc.equals(pattern, ignoreCase = true) || text.equals(pattern, ignoreCase = true)
        }
        if (matchedViewId || matchedLabel) return true

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = hasAdIndicator(child)
            child.recycle()
            if (found) return true
        }
        return false
    }

    /**
     * Recursively walks the view hierarchy looking for a skip button to click.
     * AD_SKIP_PATTERNS are always eligible; AMBIGUOUS_SKIP_PATTERNS require
     * adPlaying=true to guard against false positives on playback controls.
     */
    private fun findAndClick(node: AccessibilityNodeInfo, adPlaying: Boolean): Boolean {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val desc   = node.contentDescription?.toString() ?: ""
        val text   = node.text?.toString() ?: ""

        val isAdSkip = AD_SKIP_PATTERNS.any { pattern ->
            val p = pattern.lowercase()
            viewId.contains(p) ||
            desc.equals(pattern, ignoreCase = true) ||
            text.equals(pattern, ignoreCase = true)
        }

        val isAmbiguousSkip = !isAdSkip && adPlaying && AMBIGUOUS_SKIP_PATTERNS.any { pattern ->
            val p = pattern.lowercase()
            viewId.contains(p) ||
            desc.equals(pattern, ignoreCase = true) ||
            text.equals(pattern, ignoreCase = true)
        }

        if (isAdSkip || isAmbiguousSkip) {
            val target = if (node.isClickable) node else node.parent
            if (target != null && target.isClickable) {
                val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Clicked skip button (id='$viewId' desc='$desc' text='$text' adConfirmed=$adPlaying): success=$clicked")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findAndClick(child, adPlaying)
            child.recycle()
            if (found) return true
        }

        return false
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AdSkipper service connected")
    }
}
