package com.adskipper

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AdSkipperService : AccessibilityService() {

    companion object {
        private const val TAG = "AdSkipperService"

        /**
         * Patterns matched against viewIdResourceName, contentDescription, and text.
         *
         * HOW TO UPDATE THESE:
         * 1. Install "UI Automator Viewer" (comes with Android SDK) or run:
         *      adb shell uiautomator dump /sdcard/dump.xml && adb pull /sdcard/dump.xml
         * 2. Open YouTube, let an ad play until the skip/next button appears.
         * 3. Run the dump, open the XML, and search for the button node.
         * 4. Copy the `resource-id` and `content-desc` values into this list.
         *
         * Known values as of YouTube ~19.x (add new ones without removing old):
         */
        val SKIP_PATTERNS = listOf(
            // View IDs (resource names)
            "skip_ad_button",
            "skip_button",
            "ad_skip_button",

            // Content descriptions and text labels
            "Skip Ad",
            "Skip ad",
            "Skip Ads",
            "Skip",

        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only act on content changes and window transitions
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val root = rootInActiveWindow ?: return
        try {
            findAndClick(root)
        } finally {
            root.recycle()
        }
    }

    /**
     * Recursively walks the view hierarchy looking for a skip/next button.
     * Clicks the first match it finds.
     */
    private fun findAndClick(node: AccessibilityNodeInfo): Boolean {
        val viewId   = node.viewIdResourceName?.lowercase() ?: ""
        val desc     = node.contentDescription?.toString() ?: ""
        val text     = node.text?.toString() ?: ""

        val matched = SKIP_PATTERNS.any { pattern ->
            val p = pattern.lowercase()
            viewId.contains(p) || desc.equals(pattern, ignoreCase = true) || text.equals(pattern, ignoreCase = true)
        }

        if (matched) {
            // Prefer a directly-clickable node; climb up one level if needed
            val target = if (node.isClickable) node else node.parent
            if (target != null && target.isClickable) {
                val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Clicked skip/next (id='$viewId' desc='$desc' text='$text'): success=$clicked")
                return true
            }
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findAndClick(child)
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
