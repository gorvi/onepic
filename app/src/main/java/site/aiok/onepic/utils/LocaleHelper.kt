package site.aiok.onepic.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.*

object LocaleHelper {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "language"
    
    /**
     * 获取保存的语言代码，如果未设置则根据系统语言自动选择
     */
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        
        // 如果已保存语言设置，使用保存的值
        if (saved != null) {
            return saved
        }
        
        // 否则根据系统语言自动选择
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return when (systemLocale.language) {
            "zh" -> "zh"
            "en" -> "en"
            else -> "zh" // 默认中文
        }
    }
    
    /**
     * 保存语言设置
     */
    fun saveLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
    
    /**
     * 设置应用语言
     */
    fun setLocale(context: Context, language: String): Context {
        val locale = when (language) {
            "en" -> Locale.ENGLISH
            "zh" -> Locale.CHINESE
            else -> Locale.getDefault()
        }
        
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
    
    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return when (locale.language) {
            "en" -> "en"
            "zh" -> "zh"
            else -> "zh" // 默认中文
        }
    }
}
