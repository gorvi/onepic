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
     * 获取支持的所有语言及其显示名称
     */
    fun getSupportedLanguages(context: Context): Map<String, String> {
        val r = context.resources
        val p = context.packageName
        
        val codes = listOf(
            "en", "zh", "ja", "ko", "es", "fr", "de", "ru", "it", 
            "pt", "tr", "vi", "th", "ar", "pl", "sv", "nl", "hi"
        )
        
        val map = linkedMapOf<String, String>()
        codes.forEach { code ->
            val resName = when(code) {
                "en" -> "language_english"
                "zh" -> "language_chinese"
                "ja" -> "language_japanese"
                "ko" -> "language_korean"
                "es" -> "language_spanish"
                "fr" -> "language_french"
                "de" -> "language_german"
                "ru" -> "language_russian"
                "it" -> "language_italian"
                "pt" -> "language_portuguese"
                "tr" -> "language_turkish"
                "vi" -> "language_vietnamese"
                "th" -> "language_thai"
                "ar" -> "language_arabic"
                "pl" -> "language_polish"
                "sv" -> "language_swedish"
                "nl" -> "language_dutch"
                "hi" -> "language_hindi"
                else -> "language_english"
            }
            val resId = r.getIdentifier(resName, "string", p)
            if (resId != 0) {
                map[code] = context.getString(resId)
            }
        }
        return map
    }

    /**
     * 获取保存的语言代码，如果未设置则根据系统语言自动选择
     */
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        
        if (saved != null) return saved
        
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        val supported = listOf(
            "en", "zh", "ja", "ko", "es", "fr", "de", "ru", "it", 
            "pt", "tr", "vi", "th", "ar", "pl", "sv", "nl", "hi"
        )
        return if (supported.contains(systemLocale.language)) systemLocale.language else "en"
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
        val locale = when {
            language == "en" -> Locale.ENGLISH
            language == "zh" -> Locale.SIMPLIFIED_CHINESE
            language.contains("zh-TW") || language.contains("zh-HK") || language.contains("zh-MO") -> Locale.TRADITIONAL_CHINESE
            language == "ja" -> Locale.JAPANESE
            language == "ko" -> Locale.KOREAN
            language == "fr" -> Locale.FRENCH
            language == "de" -> Locale.GERMAN
            language == "it" -> Locale.ITALIAN
            else -> Locale(language)
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
        
        val supported = getSupportedLanguages(context).keys
        return if (supported.contains(locale.language)) locale.language else "en"
    }
}
