package com.example.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.example.accessibility.ZoyaAccessibilityService

object AccessibilityController {
    private const val TAG = "AccessibilityController"

    val service: ZoyaAccessibilityService?
        get() = ZoyaAccessibilityService.instance

    val isAvailable: Boolean
        get() = service != null

    fun getRootNode(): AccessibilityNodeInfo? {
        return try {
            service?.rootInActiveWindow
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining root node", e)
            null
        }
    }

    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val res = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                parent.recycle()
                return res
            }
            val grandParent = parent.parent
            parent.recycle()
            parent = grandParent
        }
        // If not clickable, click by bounds center coordinate
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() > 0 && rect.height() > 0) {
            return clickCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
        return false
    }

    fun clickText(targetText: String): Boolean {
        val root = getRootNode() ?: return false
        try {
            val nodes = root.findAccessibilityNodeInfosByText(targetText)
            for (node in nodes) {
                if (node.isVisibleToUser) {
                    val clicked = clickNode(node)
                    if (clicked) {
                        Log.i(TAG, "Clicked node matching '$targetText'")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding text node '$targetText'", e)
        } finally {
            try { root.recycle() } catch (e: Exception) {}
        }
        return false
    }

    fun clickCoordinates(x: Float, y: Float): Boolean {
        val serv = service ?: return false
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        return serv.dispatchGesture(gesture, null, null)
    }

    fun doubleClick(x: Float, y: Float): Boolean {
        val serv = service ?: return false
        val path = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path, 0, 50, true)
        val stroke2 = GestureDescription.StrokeDescription(path, 150, 50, false)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke1)
            .addStroke(stroke2)
            .build()
        return serv.dispatchGesture(gesture, null, null)
    }

    fun tripleClick(x: Float, y: Float): Boolean {
        val serv = service ?: return false
        val path = Path().apply { moveTo(x, y) }
        val stroke1 = GestureDescription.StrokeDescription(path, 0, 40, true)
        val stroke2 = GestureDescription.StrokeDescription(path, 120, 40, true)
        val stroke3 = GestureDescription.StrokeDescription(path, 240, 40, false)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke1)
            .addStroke(stroke2)
            .addStroke(stroke3)
            .build()
        return serv.dispatchGesture(gesture, null, null)
    }

    fun longPress(x: Float, y: Float, durationMs: Long = 800): Boolean {
        val serv = service ?: return false
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return serv.dispatchGesture(gesture, null, null)
    }

    fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, durationMs: Long = 300): Boolean {
        val serv = service ?: return false
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return serv.dispatchGesture(gesture, null, null)
    }

    fun scroll(direction: String): Boolean {
        val root = getRootNode() ?: return false
        try {
            val action = if (direction.equals("up", ignoreCase = true)) {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            fun findAndScroll(node: AccessibilityNodeInfo): Boolean {
                if (node.isScrollable) {
                    return node.performAction(action)
                }
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    if (findAndScroll(child)) {
                        child.recycle()
                        return true
                    }
                    child.recycle()
                }
                return false
            }
            if (findAndScroll(root)) return true

            // Gesture fallback swipe
            val serv = service ?: return false
            val metrics = serv.resources.displayMetrics
            val centerX = metrics.widthPixels / 2f
            val startY = if (direction.equals("up", ignoreCase = true)) metrics.heightPixels * 0.3f else metrics.heightPixels * 0.7f
            val endY = if (direction.equals("up", ignoreCase = true)) metrics.heightPixels * 0.7f else metrics.heightPixels * 0.3f
            return swipe(centerX, startY, centerX, endY, 350)
        } finally {
            try { root.recycle() } catch (e: Exception) {}
        }
    }

    fun inputText(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun pressBack(): Boolean {
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) ?: false
    }

    fun pressHome(): Boolean {
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) ?: false
    }

    fun pressRecents(): Boolean {
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) ?: false
    }

    fun openNotifications(): Boolean {
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) ?: false
    }

    fun openQuickSettings(): Boolean {
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS) ?: false
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) ?: false
        } else {
            false
        }
    }
}
