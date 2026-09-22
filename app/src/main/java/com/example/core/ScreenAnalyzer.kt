package com.example.core

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object ScreenAnalyzer {
    private const val TAG = "ScreenAnalyzer"

    fun getCurrentPackage(): String {
        val root = AccessibilityController.getRootNode() ?: return ""
        val pkg = root.packageName?.toString() ?: ""
        try { root.recycle() } catch (e: Exception) {}
        return pkg
    }

    fun isAppInForeground(packageNameSubstring: String): Boolean {
        val current = getCurrentPackage().lowercase()
        return current.contains(packageNameSubstring.lowercase())
    }

    fun hasTextOnScreen(targetText: String, ignoreCase: Boolean = true): Boolean {
        val root = AccessibilityController.getRootNode() ?: return false
        try {
            val list = mutableListOf<String>()
            collectNodeTexts(root, list)
            return list.any {
                if (ignoreCase) it.contains(targetText, ignoreCase = true)
                else it.contains(targetText)
            }
        } finally {
            try { root.recycle() } catch (e: Exception) {}
        }
    }

    fun getAllVisibleTexts(): List<String> {
        val root = AccessibilityController.getRootNode() ?: return emptyList()
        val list = mutableListOf<String>()
        try {
            collectNodeTexts(root, list)
        } finally {
            try { root.recycle() } catch (e: Exception) {}
        }
        return list
    }

    private fun collectNodeTexts(node: AccessibilityNodeInfo, output: MutableList<String>) {
        if (!node.isVisibleToUser) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank()) output.add(text)
        if (!desc.isNullOrBlank() && desc != text) output.add(desc)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectNodeTexts(child, output)
            child.recycle()
        }
    }

    /**
     * Checks if a YouTube video is currently playing on the screen.
     * Looks for YouTube package in foreground, presence of pause button (indicating video is currently active/playing),
     * or active player container.
     */
    fun isYouTubeVideoPlaying(): Boolean {
        val pkg = getCurrentPackage()
        if (!pkg.contains("youtube", ignoreCase = true)) return false

        val texts = getAllVisibleTexts()
        // In YouTube, when video plays, player controls show "Pause video" or playback time
        val hasPause = texts.any {
            it.contains("pause", ignoreCase = true) ||
            it.contains("विराम", ignoreCase = true) ||
            it.contains("pause video", ignoreCase = true)
        }
        val hasPlayer = texts.any {
            it.contains("watch", ignoreCase = true) ||
            it.contains("subscriber", ignoreCase = true) ||
            it.contains("views", ignoreCase = true) ||
            it.matches(Regex(".*[0-9]+:[0-9]+.*")) // Timestamp e.g. 0:15 / 3:45
        }
        return hasPause || hasPlayer
    }

    /**
     * Detects if an on-screen dialog, alert popup, or keyboard is showing.
     */
    fun isDialogOrKeyboardShowing(): Boolean {
        val root = AccessibilityController.getRootNode() ?: return false
        try {
            val className = root.className?.toString() ?: ""
            if (className.contains("Dialog", ignoreCase = true) || className.contains("Alert", ignoreCase = true)) {
                return true
            }
            // Check for input method window
            val pkg = root.packageName?.toString() ?: ""
            if (pkg.contains("inputmethod", ignoreCase = true) || pkg.contains("gboard", ignoreCase = true)) {
                return true
            }
        } finally {
            try { root.recycle() } catch (e: Exception) {}
        }
        return false
    }
}
