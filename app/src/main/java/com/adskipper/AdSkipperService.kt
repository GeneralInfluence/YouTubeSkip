package com.adskipper

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AdSkipperService : AccessibilityService() {

    companion object {
        private const val TAG = "AdSkipperService"

        private const val YOUTUBE_PKG = "com.google.android.youtube"

        /**
         * How often to poll YouTube's PiP window for a skip button. Polling is
         * required because the service is otherwise event-driven, and the app in
         * the foreground while YouTube is minimized (e.g. a full-screen game) may
         * emit few or no accessibility events — so onAccessibilityEvent would
         * never fire to trigger a scan.
         */
        private const val PIP_POLL_INTERVAL_MS = 1000L

        /** Minimum gap between two identical on-screen diagnostic toasts. */
        private const val TOAST_THROTTLE_MS = 4000L

        /**
         * Set false to silence the on-screen diagnostic toasts once PiP skipping
         * is confirmed working. They exist to show the user (without adb) exactly
         * what the service can see in the PiP window.
         */
        private const val DIAGNOSTICS = true

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

        /**
         * A node is NEVER clicked if it looks like a next/previous navigation
         * control — those advance or rewind the video (the "it jumped to the
         * next video" bug). Matched via contains() on id/desc/text.
         */
        val NAV_EXCLUDE_PATTERNS = listOf(
            "next",
            "previous",
            "prev_button"
        )
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Repeatedly scans YouTube's PiP window; reschedules itself. */
    private val pipPoller = object : Runnable {
        override fun run() {
            try {
                scanYouTubePip()
            } catch (e: Exception) {
                Log.e(TAG, "PiP poll failed", e)
            } finally {
                handler.postDelayed(this, PIP_POLL_INTERVAL_MS)
            }
        }
    }

    // Toast de-duplication + PiP-presence edge detection.
    private var lastToastMsg = ""
    private var lastToastAt = 0L
    private var pipWasPresent = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        // Foreground YouTube (full-screen): scan the active window on each event
        // for responsive skipping. Only ever act on YouTube's own window.
        rootInActiveWindow?.let { root ->
            try {
                if (root.packageName == YOUTUBE_PKG) {
                    findAndClick(root, hasAdIndicator(root))
                }
            } finally {
                root.recycle()
            }
        }
        // The PiP case is handled by the timer (pipPoller), not here — the
        // foreground app may emit no events to drive an event-based scan.
    }

    /**
     * Scans YouTube's picture-in-picture window (if any) for a skip button.
     * Runs on a timer so it works even when the foreground app is silent.
     * Requires API 26+ and flagRetrieveInteractiveWindows.
     */
    private fun scanYouTubePip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        var pipPresent = false
        windows
            .filter { it.isInPictureInPictureMode }
            .forEach { window ->
                val pipRoot = window.root ?: return@forEach
                try {
                    if (pipRoot.packageName == YOUTUBE_PKG) {
                        pipPresent = true
                        val adPlaying = hasAdIndicator(pipRoot)
                        val clicked = findAndClick(pipRoot, adPlaying)
                        when {
                            clicked  -> diagToast("AdSkipper: skipped ad in mini-player ✓")
                            adPlaying -> diagToast("AdSkipper: ad in mini-player, but no skip button is accessible")
                        }
                    }
                } finally {
                    pipRoot.recycle()
                }
            }

        // Announce once when the mini-player appears, so the user can confirm
        // the service is actually watching it (vs. not detecting it at all).
        if (pipPresent && !pipWasPresent) {
            diagToast("AdSkipper: watching YouTube mini-player")
        }
        pipWasPresent = pipPresent
    }

    /** Shows a short on-screen message, throttled so identical toasts don't spam. */
    private fun diagToast(msg: String) {
        if (!DIAGNOSTICS) return
        val now = SystemClock.uptimeMillis()
        if (msg == lastToastMsg && now - lastToastAt < TOAST_THROTTLE_MS) return
        lastToastMsg = msg
        lastToastAt = now
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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

        // Never click a next/previous navigation control, even if it happens to
        // also match a skip pattern — advancing the video is never what we want.
        val isNavControl = NAV_EXCLUDE_PATTERNS.any { p ->
            viewId.contains(p) ||
            desc.contains(p, ignoreCase = true) ||
            text.contains(p, ignoreCase = true)
        }

        val isAdSkip = !isNavControl && AD_SKIP_PATTERNS.any { pattern ->
            val p = pattern.lowercase()
            viewId.contains(p) ||
            desc.equals(pattern, ignoreCase = true) ||
            text.equals(pattern, ignoreCase = true)
        }

        val isAmbiguousSkip = !isNavControl && !isAdSkip && adPlaying && AMBIGUOUS_SKIP_PATTERNS.any { pattern ->
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
                // Diagnostic: show exactly what was clicked so a false positive
                // can be identified by its real id/desc/text without adb.
                diagToast("AdSkipper clicked → id='${viewId.substringAfterLast('/')}' desc='$desc' text='$text' ad=$adPlaying")
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
        handler.removeCallbacks(pipPoller)
        handler.postDelayed(pipPoller, PIP_POLL_INTERVAL_MS)
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        handler.removeCallbacks(pipPoller)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pipPoller)
        super.onDestroy()
    }
}
