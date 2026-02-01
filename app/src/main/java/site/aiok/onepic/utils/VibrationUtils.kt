package site.aiok.onepic.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object VibrationUtils {
    private const val PREFS_NAME = "vibration_settings"
    private const val KEY_ENABLED = "vibration_enabled"

    /**
     * Check if vibration is enabled.
     */
    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true) // Default: enabled
    }

    /**
     * Enable or disable vibration.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Vibrate for a short duration (e.g., for unlocking a level)
     */
    fun vibrate(context: Context, durationMs: Long = 50) {
        Log.d("VibrationUtils", "vibrate() called, durationMs=$durationMs")
        if (!isEnabled(context)) {
            Log.d("VibrationUtils", "Vibration is DISABLED in settings, skipping.")
            return
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        Log.d("VibrationUtils", "Vibrator obtained, triggering vibration...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
        Log.d("VibrationUtils", "Vibration triggered successfully.")
    }
}
