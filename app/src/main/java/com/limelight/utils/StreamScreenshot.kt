package com.limelight.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.annotation.StringRes
import com.limelight.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.hypot

/**
 * Touch gesture configured by the player to capture an in-stream screenshot without triggering
 * Android's system gesture bar.
 */
enum class ScreenshotGesture(val prefValue: String, @StringRes val labelRes: Int) {
    OFF("off", R.string.settings_native_screenshot_gesture_off),
    TWO_FINGER_TAP("two_finger_tap", R.string.settings_native_screenshot_gesture_2tap),
    THREE_FINGER_TAP("three_finger_tap", R.string.settings_native_screenshot_gesture_3tap),
    FOUR_FINGER_TAP("four_finger_tap", R.string.settings_native_screenshot_gesture_4tap),
    PINCH_IN("pinch_in", R.string.settings_native_screenshot_gesture_pinch);

    fun step(dir: Int): ScreenshotGesture {
        val list = entries
        return list[((ordinal + dir) % list.size + list.size) % list.size]
    }

    companion object {
        const val KEY_GESTURE = "ui_screenshot_gesture"
        const val KEY_INCLUDE_OVERLAYS = "ui_screenshot_include_overlays"

        @JvmStatic
        fun readGesture(prefs: SharedPreferences): ScreenshotGesture {
            val saved = prefs.getString(KEY_GESTURE, null) ?: return OFF
            return entries.firstOrNull { it.prefValue == saved || it.name == saved } ?: OFF
        }

        @JvmStatic
        fun writeGesture(prefs: SharedPreferences, gesture: ScreenshotGesture) {
            prefs.edit().putString(KEY_GESTURE, gesture.prefValue).apply()
        }

        @JvmStatic
        fun readIncludeOverlays(prefs: SharedPreferences): Boolean =
            prefs.getBoolean(KEY_INCLUDE_OVERLAYS, false)

        @JvmStatic
        fun writeIncludeOverlays(prefs: SharedPreferences, include: Boolean) {
            prefs.edit().putBoolean(KEY_INCLUDE_OVERLAYS, include).apply()
        }
    }
}

/**
 * Recognizes multi-finger tap and pinch-in screenshot gestures from raw [MotionEvent] streams.
 */
class ScreenshotGestureDetector(
    private val gesture: ScreenshotGesture,
    private val density: Float = 2f,
    private val onTrigger: () -> Unit,
) {
    private var active = false
    private var maxPointers = 0
    private var startTimeMs = 0L
    private var movedTooFar = false
    private var pinchStartDist = -1f
    private var pinchFired = false
    private val startX = FloatArray(MAX_TRACKED_POINTERS)
    private val startY = FloatArray(MAX_TRACKED_POINTERS)

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (gesture == ScreenshotGesture.OFF) return false
        val count = event.pointerCount.coerceAtMost(MAX_TRACKED_POINTERS)
        val xs = FloatArray(count) { i -> event.getX(i) }
        val ys = FloatArray(count) { i -> event.getY(i) }
        return onMotion(event.actionMasked, event.pointerCount, event.eventTime, xs, ys)
    }

    internal fun onMotion(
        actionMasked: Int,
        pointerCount: Int,
        eventTimeMs: Long,
        xs: FloatArray,
        ys: FloatArray,
    ): Boolean {
        if (gesture == ScreenshotGesture.OFF) return false

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                active = true
                maxPointers = 1
                startTimeMs = eventTimeMs
                movedTooFar = false
                pinchStartDist = -1f
                pinchFired = false
                if (xs.isNotEmpty() && ys.isNotEmpty()) {
                    startX[0] = xs[0]
                    startY[0] = ys[0]
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!active) return false
                if (pointerCount > maxPointers) maxPointers = pointerCount
                val idx = (pointerCount - 1).coerceIn(0, MAX_TRACKED_POINTERS - 1)
                if (idx < xs.size && idx < ys.size) {
                    startX[idx] = xs[idx]
                    startY[idx] = ys[idx]
                }
                if (gesture == ScreenshotGesture.PINCH_IN && pointerCount == 2 && xs.size >= 2 && ys.size >= 2) {
                    pinchStartDist = hypot((xs[0] - xs[1]).toDouble(), (ys[0] - ys[1]).toDouble()).toFloat()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!active) return false
                if (gesture == ScreenshotGesture.PINCH_IN) {
                    if (pointerCount == 2 && !pinchFired && xs.size >= 2 && ys.size >= 2) {
                        val currentDist = hypot((xs[0] - xs[1]).toDouble(), (ys[0] - ys[1]).toDouble()).toFloat()
                        if (pinchStartDist >= MIN_PINCH_SPAN_DP * density && currentDist <= pinchStartDist * PINCH_RATIO) {
                            pinchFired = true
                            onTrigger()
                            return true
                        }
                    }
                    return false
                }
                val slop = TAP_SLOP_DP * density
                val tracked = minOf(pointerCount, xs.size, ys.size, MAX_TRACKED_POINTERS)
                for (i in 0 until tracked) {
                    if (hypot((xs[i] - startX[i]).toDouble(), (ys[i] - startY[i]).toDouble()) > slop) {
                        movedTooFar = true
                        break
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!active) return false
                val targetCount = when (gesture) {
                    ScreenshotGesture.TWO_FINGER_TAP -> 2
                    ScreenshotGesture.THREE_FINGER_TAP -> 3
                    ScreenshotGesture.FOUR_FINGER_TAP -> 4
                    else -> -1
                }
                val triggered = targetCount > 0 &&
                    maxPointers == targetCount &&
                    !movedTooFar &&
                    (eventTimeMs - startTimeMs) <= TAP_TIMEOUT_MS
                reset()
                if (triggered) {
                    onTrigger()
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> reset()
        }
        return false
    }

    private fun reset() {
        active = false
        maxPointers = 0
        startTimeMs = 0L
        movedTooFar = false
        pinchStartDist = -1f
        pinchFired = false
    }

    private companion object {
        const val MAX_TRACKED_POINTERS = 8
        const val TAP_TIMEOUT_MS = 350L
        const val TAP_SLOP_DP = 20f
        const val MIN_PINCH_SPAN_DP = 48f
        const val PINCH_RATIO = 0.65f
    }
}

/**
 * Captures the active stream frame (either the raw video surface or the full app window with
 * overlays) via [PixelCopy] and saves it into `Pictures/DELOS` via [MediaStore].
 */
object StreamScreenshot {
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun capture(activity: Activity, surfaceView: SurfaceView?, includeOverlays: Boolean, appName: String?) {
        val window = activity.window ?: return
        val targetSurface = if (!includeOverlays && surfaceView != null &&
            surfaceView.width > 0 && surfaceView.height > 0 &&
            surfaceView.holder.surface.isValid
        ) surfaceView else null
        val width = targetSurface?.width ?: window.decorView.width
        val height = targetSurface?.height ?: window.decorView.height
        if (width <= 0 || height <= 0) return

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val listener = PixelCopy.OnPixelCopyFinishedListener { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                saveBitmap(activity.applicationContext, bitmap, appName)
            } else {
                bitmap.recycle()
                mainHandler.post {
                    DelosToast.show(activity, R.string.stream_screenshot_failed, DelosToast.SHORT, ToastKind.ERROR)
                }
            }
        }
        try {
            if (targetSurface != null) {
                PixelCopy.request(targetSurface, bitmap, listener, mainHandler)
            } else {
                PixelCopy.request(window, bitmap, listener, mainHandler)
            }
        } catch (_: IllegalArgumentException) {
            bitmap.recycle()
            DelosToast.show(activity, R.string.stream_screenshot_failed, DelosToast.SHORT, ToastKind.ERROR)
        }
    }

    private fun saveBitmap(context: Context, bitmap: Bitmap, appName: String?) {
        ioExecutor.execute {
            var saved = false
            try {
                val cleanApp = (appName ?: "DELOS")
                    .replace(Regex("[^A-Za-z0-9_-]+"), "_")
                    .trim('_')
                    .ifEmpty { "DELOS" }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "DELOS_${cleanApp}_$stamp.png"
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DELOS")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            } catch (_: Exception) {
                saved = false
            } finally {
                bitmap.recycle()
            }
            val msgRes = if (saved) R.string.stream_screenshot_saved else R.string.stream_screenshot_failed
            val kind = if (saved) ToastKind.SUCCESS else ToastKind.ERROR
            mainHandler.post {
                DelosToast.show(context, msgRes, DelosToast.SHORT, kind)
            }
        }
    }
}
