package site.aiok.onepic.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import site.aiok.onepic.R

/**
 * Helper object to satisfy the requirement:
 * ALWAYS use the TRANS.get("key", "Default Text") pattern or i18n_utils to load strings.
 *
 * This implementation bridges the key-based pattern to Android resources.
 */
object TRANS {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Get string by key.
     * Tries to find a resource ID with the given name (key).
     * If found, returns the localized string.
     * If not found, returns the default value.
     */
    fun get(key: String, defaultValue: String): String {
        val context = appContext ?: return defaultValue
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            defaultValue
        }
    }
    
    @Composable
    fun get(key: String, defaultValue: String, context: Context = LocalContext.current): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            defaultValue
        }
    }
}
