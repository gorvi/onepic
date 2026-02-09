import Foundation
import SwiftUI

class LevelRepository {
    static let shared = LevelRepository()
    
    // Cache
    private var cachedGalleryLevels: [LevelConfig]?
    private var storyDataMap: [Int: StoryData] = [:]
    
    struct StoryData: Decodable {
        let level_id: Int
        let module_name_zh: String
        let status: String
        let motivation: String
        let log: String
    }
    
    private init() {
        loadStoryData()
    }
    
    // MARK: - Shared Data
    // Chapter and Module data handled via TRANS.get with fallback defaults
    
    func getChapterName(chapter: Int) -> String {
        if chapter == 0 { return TRANS.get("level_tutorial_title", "Chapter 0: Awakening") }
        if chapter >= 1 && chapter <= 12 {
            // Use Android-compatible key format: chapter_1, chapter_2, etc.
            return TRANS.get("chapter_\(chapter)", "Chapter \(chapter)")
        }
        return TRANS.get("chapter_unknown", "Unknown Sector")
    }

    // MARK: - ID Generators (Android Parity)
    
    static func makeMainId(index: Int) -> String {
        if index == 0 { return "tutorial_0" }
        return "g_\(index)_A"
    }
    
    static func makeAscendedId(index: Int) -> String {
        if index == 0 { return "tutorial_0_B" }
        return "g_\(index)_B"
    }
    
    // MARK: - Single Source of Truth Logic
    
    func getClassicLevels() -> [LevelConfig] {
        if let cache = cachedGalleryLevels, !cache.isEmpty {
            return cache
        }
        
        var levels: [LevelConfig] = []
        
        // 0. Add Tutorial First
        levels.append(getTutorialLevel())
        
        // 1. Load JSONs (directly, no tutorial)
        guard let galleryData = loadGalleryJSON() else {
            print("❌ Failed to load gallery JSON, using Fallback")
            // Fallback: Return manual levels 1-5 so UI isn't empty
            return levels + getFallbackLevels()
        }
        
        // 2. Parse 60 Main Levels
        let count = min(60, galleryData.count)
        
        for i in 0..<count {
            if let dict = galleryData[i] as? [String: Any] {
                let id = i + 1
                let levelId = Self.makeMainId(index: id)
                
                let rawFilename = dict["filename_a"] as? String ?? ""
                let basename = rawFilename.replacingOccurrences(of: ".jpg", with: "").replacingOccurrences(of: ".webp", with: "")
                
                let imagePath = "gallery_levels/\(basename).webp"
                // Fix: JSON key is "description", not "title"
                let title = dict["description"] as? String ?? "Level \(id)"
                let richStory = dict["story_text"] as? String ?? ""
                
                let (rows, cols) = getGridSize(for: id)
                let diff = getDifficulty(for: id)
                let moduleName = determineModuleName(id: id)
                
                let level = LevelConfig(
                    levelId: levelId,
                    title: title,
                    difficulty: diff,
                    imageSource: .asset(path: imagePath),
                    rows: rows,
                    cols: cols,
                    storyText: richStory,
                    animationTheme: determineTheme(id: id),
                    isAscended: false,
                    moduleName: moduleName,
                    integrityStatus: TRANS.get("integrity_standard", "Standard Integrity (100%)"),
                    motivation: nil
                )
                levels.append(level)
            }
        }
        
        cachedGalleryLevels = levels
        return levels
    }
    
    // Reset cache when language changes
    func resetCache() {
        cachedGalleryLevels = nil
        // Also clear storyDataMap if we decide to reload it (though currently it's init-only)
        // storyDataMap = [:] 
        // loadStoryData() // Uncomment if we make loadStoryData dynamic
        print("♻️ LevelRepository cache cleared")
    }
    
    /// 获取当前关卡完成后的下一关 levelId
    func getNextLevelId(after levelId: String) -> String? {
        let levels = getClassicLevels()
        guard let idx = levels.firstIndex(where: { $0.levelId == levelId }), idx + 1 < levels.count else {
            return nil
        }
        return levels[idx + 1].levelId
    }
    
    // MARK: - Ascended Levels (120 = 60 Main + 60 Ascended)
    
    /// Returns all 120 levels interleaved: (Level 1, Level 1 Ascended, Level 2, Level 2 Ascended, ...)
    /// Mirrors Android GalleryScreen sorting logic.
    func getAllGalleryLevels() -> [LevelConfig] {
        let classicLevels = getClassicLevels()
        var result: [LevelConfig] = []
        
        for (index, classicLevel) in classicLevels.enumerated() {
            // Android Parity: The 120 levels refer to 60 Main + 60 Ascended.
            // Tutorial (index 0) is excluded from the Gallery/Memory sequence.
            if index == 0 { continue }
            
            result.append(classicLevel)
            
            // Add corresponding Ascended level
            if let ascended = getAscendedLevel(mainIndex: index) {
                result.append(ascended)
            }
        }
        return result
    }
    
    /// 主关卡 index：0=Tutorial, 1-60=主关卡。完成主关卡后出现对应升级关卡。
    func getAscendedLevel(mainIndex: Int) -> LevelConfig? {
        let galleryData = loadGalleryJSON() ?? []
        let levelId: String
        
        if mainIndex == 0 {
            levelId = Self.makeAscendedId(index: 0)
            return LevelConfig(
                levelId: levelId,
                title: TRANS.get("level_tutorial_title_asc", "Chapter 0: Awakening+"),
                difficulty: TRANS.get("diff_normal", "Normal"), // Tutorial+ is arguably Normal
                imageSource: .asset(path: "gallery_levels/level_00_B_TheOrigin"),
                rows: 3,
                cols: 3,
                storyText: TRANS.get("level_tutorial_story_asc", "The next phase begins..."),
                animationTheme: "mechanical",
                isAscended: true,
                moduleName: "Awakening+",
                integrityStatus: TRANS.get("integrity_quantum", "Quantum Integrity (200%)"),
                motivation: nil
            )
        }
        
        guard mainIndex >= 1, mainIndex <= 60, mainIndex <= galleryData.count,
              let dict = galleryData[mainIndex - 1] as? [String: Any] else {
            return nil
        }
        
        levelId = Self.makeAscendedId(index: mainIndex)
        let filenameB = (dict["filename_b"] as? String ?? "").replacingOccurrences(of: ".jpg", with: "")
        if filenameB.isEmpty { return nil }
        // Fix: JSON key is "description", not "title"
        let title = (dict["description"] as? String ?? "Level \(mainIndex)") + TRANS.get("suffix_ascended", " (Ascended)")
        let virtualId = 60 + mainIndex
        let sciFiLog = storyDataMap[virtualId]?.log ?? "System Data Corrupted."
        let (rows, cols) = getAscendedGridSize(for: mainIndex)
        let diff = getAscendedDifficulty(for: mainIndex)
        let moduleName = determineModuleName(id: virtualId)
        
        return LevelConfig(
            levelId: levelId,
            title: title,
            difficulty: diff,
            imageSource: .asset(path: "gallery_levels/\(filenameB)"),
            rows: rows,
            cols: cols,
            storyText: sciFiLog,
            animationTheme: determineTheme(id: mainIndex),
            isAscended: true,
            moduleName: moduleName,
            integrityStatus: TRANS.get("integrity_quantum", "Quantum Integrity (200%)"),
            motivation: nil
        )
    }
    
    private func getAscendedGridSize(for mainId: Int) -> (Int, Int) {
        switch mainId {
        case 1...10: return (4, 5)
        case 11...20: return (5, 5)
        case 21...30: return (5, 6)
        case 31...40: return (6, 6)
        case 41...50: return (6, 7)
        default: return (7, 7)
        }
    }
    
    private func getAscendedDifficulty(for mainId: Int) -> String {
        switch mainId {
        case 1...20: return TRANS.get("diff_hard", "Hard")
        case 21...40: return TRANS.get("diff_expert", "Expert")
        default: return TRANS.get("diff_master", "Master")
        }
    }
    
    // MARK: - Helpers
    
    private func getTutorialLevel() -> LevelConfig {
        return LevelConfig(
            levelId: "tutorial_0",
            title: TRANS.get("level_tutorial_title", "Chapter 0: Awakening"),
            difficulty: TRANS.get("diff_tutorial", "Tutorial"),
            imageSource: .asset(path: "gallery_levels/level_00_A_TheOrigin"),
            rows: 2,
            cols: 2,
            storyText: TRANS.get("level_tutorial_story", "The Eternal Voyager awakens..."),
            animationTheme: "mechanical",
            isAscended: false,
            moduleName: "Awakening",
            integrityStatus: TRANS.get("integrity_init", "System Init"),
            motivation: nil
        )
    }
    
    private func loadGalleryJSON() -> [Any]? {
        // Use single source of truth for language
        let langCode = LevelProgressManager.shared.getSelectedLanguage()
        
        // Priority: zh-Hans, zh, then en
        // Priority: Match filename exactly if possible, else fallback
        var targetName = "en"
        
        if langCode == "zh-HK" { targetName = "zh-rHK" }
        else if langCode == "zh-TW" { targetName = "zh-rTW" }
        else if langCode == "zh-MO" { targetName = "zh-rMO" }
        else if langCode.hasPrefix("zh") {
             // simplified chinese -> zh.json
             targetName = "zh"
        } else {
             targetName = langCode
        }
        
        // Try strict paths first (Folder References)
        let possibleSubdirs = [
            "gallery_levels/i18n/level_content",
            "Resources/gallery_levels/i18n/level_content", 
            "gallery_levels/i18n" // Legacy fallback
        ]
        
        for subdir in possibleSubdirs {
            if let url = Bundle.main.url(forResource: targetName, withExtension: "json", subdirectory: subdir) {
                if let data = try? Data(contentsOf: url),
                   let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [Any] {
                    return json
                }
            }
        }
        
        // Recursive / Direct Search as fallback
        if let url = findResourceURL(name: targetName, ext: "json") {
            if let data = try? Data(contentsOf: url),
               let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [Any] {
                return json
            }
        }
        
        return nil
    }
    
    private func loadJSON(name: String) -> [Any]? {
        // Redundant with above logic update, removing specific helper usage or repurposing
        return nil 
    }
    
    private func loadStoryData() {
        // Try precise, then root, then generic search
        if let url = findResourceURL(name: "project_exodus_story", ext: "json") {
            do {
                let data = try Data(contentsOf: url)
                let items = try JSONDecoder().decode([StoryData].self, from: data)
                for item in items {
                    storyDataMap[item.level_id] = item
                }
                print("✅ Story Data Loaded from \(url.lastPathComponent)")
            } catch {
                print("❌ Story Data Parse Error: \(error)")
            }
        } else {
             print("⚠️ Story Data JSON not found in Bundle via Recursive Search")
        }
    }
    
    private func getGridSize(for id: Int) -> (Int, Int) {
        switch id {
        case 1...10: return (3, 4)
        case 11...20: return (4, 4)
        case 21...30: return (4, 5)
        case 31...40: return (5, 5)
        case 41...50: return (5, 6)
        default: return (6, 6)
        }
    }
    
    private func getDifficulty(for id: Int) -> String {
        switch id {
        case 1...20: return TRANS.get("diff_easy", "Easy")
        case 21...40: return TRANS.get("diff_normal", "Normal")
        default: return TRANS.get("diff_intermediate", "Intermediate")
        }
    }
    
    private func determineTheme(id: Int) -> String {
        // Simple mapping based on mapping in Android LevelRepository logic implicitly or via strings
        // Based on chapter map in earlier turn:
        // 0: light, 1: light, 2: nature, etc.
        // Let's use the explicit switch from mapping
        let chapter = (id - 1) / 5 + 1
        switch chapter {
        case 1: return "light"
        case 2: return "nature"
        case 3: return "abyss"
        case 4: return "fire"
        case 5: return "magic"
        case 6: return "ruins"
        case 7: return "ice"
        case 8: return "mechanical"
        case 9: return "time"
        case 10: return "cosmos"
        case 11: return "void"
        case 12: return "unity"
        default: return "magic"
        }
    }
    
    // MARK: - Resource Finder
    
    private func findResourceURL(name: String, ext: String, subdirectory: String? = nil) -> URL? {
        // 1. Precise Lookup
        if let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: subdirectory) {
            return url
        }
        
        // 2. Root Lookup
        if let url = Bundle.main.url(forResource: name, withExtension: ext) {
            return url
        }
        
        // 3. Recursive Search (The Nuclear Option)
        // This helps find files buried in Folder References (Blue folders)
        let fileManager = FileManager.default
        if let resourcePath = Bundle.main.resourcePath {
            let enumerator = fileManager.enumerator(atPath: resourcePath)
            while let file = enumerator?.nextObject() as? String {
                if file.hasSuffix("\(name).\(ext)") {
                    return Bundle.main.bundleURL.appendingPathComponent(file)
                }
            }
        }
        
        return nil
    }

    private func determineModuleName(id: Int) -> String {
        let baseId = id > 60 ? id - 60 : id
        let idx = (baseId - 1) / 5
        let modules = [
            "Quantum Keel", "Fusion Reactor Heart", "Neuro-Link Cockpit",
            "Cryostasis Hall", "The Bio-Dome", "Void Shields",
            "Hyper-Sensors", "Ion Thrusters", "Communication Spire",
            "Genesis Library", "Warp Drive", "The Launch Key"
        ]
        
        if idx < modules.count {
            // Use 1-based index for TRANS keys matching "module_1", "module_2", etc.
            let moduleKey = "module_\(idx + 1)"
            return TRANS.get(moduleKey, modules[idx])
        }
        return TRANS.get("module_unknown", "Unknown Module")
    }
    
    private func getFallbackLevels() -> [LevelConfig] {
        var fallbacks: [LevelConfig] = []
        for i in 1...10 {
            fallbacks.append(LevelConfig(
                levelId: Self.makeMainId(index: i),
                title: "Level \(i)",
                difficulty: "Easy",
                imageSource: .asset(path: "gallery_levels/level_\(String(format: "%02d", i))_A_WorldTreeSeeds"), // Attempt basic name
                rows: 3,
                cols: 4,
                storyText: "Fallback Story...",
                animationTheme: "light",
                isAscended: false,
                moduleName: determineModuleName(id: i),
                integrityStatus: "100%",
                motivation: nil
            ))
        }
        return fallbacks
    }
}
