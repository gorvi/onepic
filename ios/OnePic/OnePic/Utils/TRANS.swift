import Foundation

/// Helper struct to satisfy the requirement:
/// ALWAYS use the TRANS.get("key", "Default Text") pattern.
struct TRANS {
    /// Get string by key with support for in-app language switching.
    /// - Parameters:
    ///   - key: The key in Localizable.strings
    ///   - defaultValue: The value to return if the key is not found
    /// - Returns: Localized string from the selected bundle
    static func get(_ key: String, _ defaultValue: String) -> String {
        let selectedLang = LevelProgressManager.shared.getSelectedLanguage()
        let dict = loadTranslations(for: selectedLang)
        
        if let val = dict[key] {
            return val
        }
        
        // Default value provided in code
        return defaultValue
    }
    
    // MARK: - Dynamic JSON Loading
    
    /// Internal cache for loaded translations
    private static var loadedTranslations: [String: String]? = nil
    private static var currentLoadedLanguage: String? = nil
    
    private static func loadTranslations(for langCode: String) -> [String: String] {
        // Return cached if available and language hasn't changed
        if let loaded = loadedTranslations, currentLoadedLanguage == langCode {
            return loaded
        }
        
        var combined: [String: String] = [:]
        
        // --- Helper to try loading from multiple possible locations ---
        func tryLoad(filename: String) -> [String: String]? {
            let possibleSubdirs = ["gallery_levels/i18n/ui", "i18n/ui", nil]
            for subdir in possibleSubdirs {
                if let url = Bundle.main.url(forResource: filename, withExtension: "json", subdirectory: subdir),
                   let data = try? Data(contentsOf: url),
                   let dict = try? JSONDecoder().decode([String: String].self, from: data) {
                    print("✅ TRANS loaded: \(filename) from \(subdir ?? "root")")
                    return dict
                }
            }
            return nil
        }

        // 1. Always load English as Base Fallback
        if let enDict = tryLoad(filename: "ui_en_data") {
            combined.merge(enDict) { (_, new) in new }
        }
        
        // 2. Normalize and Load Target Language
        // Handle "en-US", "fr-FR" -> "en", "fr"
        var baseCode = langCode.split(separator: "-").first.map(String.init) ?? langCode
        if langCode.contains("_") {
            baseCode = langCode.split(separator: "_").first.map(String.init) ?? baseCode
        }

        var targetParams = baseCode
        if langCode == "zh-HK" || langCode.contains("HK") { targetParams = "zh-rHK" }
        else if langCode == "zh-TW" || langCode.contains("TW") { targetParams = "zh-rTW" }
        else if langCode == "zh-MO" || langCode.contains("MO") { targetParams = "zh-rMO" }
        else if langCode.hasPrefix("zh") { 
            // Simplified fallback for all other zh
            targetParams = "zh" 
        }

        // Load specific target if it's not the redundant "en" base
        if targetParams != "en" {
            if let targetDict = tryLoad(filename: "ui_\(targetParams)_data") {
                combined.merge(targetDict) { (_, new) in new }
            }
        }
        
        // Update Cache
        loadedTranslations = combined
        currentLoadedLanguage = langCode
        print("🌍 TRANS final dictionary key count: \(combined.count) for: \(langCode) (mapped to \(targetParams))")
        return combined
    }

    /// Reloads translations (call this when language changes)
    static func resetCache() {
        loadedTranslations = nil
        currentLoadedLanguage = nil
    }

    // MARK: - Static Dictionary (Deprecated / Fallback)
    // We keep this empty or minimal as we now rely on JSON
    static let translations: [String: [String: String]] = [:]
}

