package site.aiok.onepic.data

import android.content.Context
import android.net.Uri
import site.aiok.onepic.model.ImageSource
import site.aiok.onepic.model.LevelConfig

object LevelRepository {

    // --- Mode 1: Classic (61 Levels: 1 Tutorial + 60 Gallery) ---
    fun getTutorialLevel(context: Context) = LevelConfig(
        levelId = "tutorial_0", 
        title = context.getString(site.aiok.onepic.R.string.level_tutorial_title), 
        difficulty = "Tutorial", 
        imageSource = ImageSource.Asset("gallery_levels/level_00_A_TheOrigin.jpg"), 
        rows = 2, 
        cols = 2,
        storyText = context.getString(site.aiok.onepic.R.string.level_tutorial_story),
        animationTheme = "mechanical",
        isAscended = false
    )

    // classicLevels is now dynamically computed via getClassicLevels(context)
    // Backwards compatibility for existing code accessing 'levels' - DEPRECATED
    @Deprecated("Use getClassicLevels(context) instead")
    val levels: List<LevelConfig> = emptyList()

    fun getLevel(context: Context, id: String): LevelConfig? {
        if (id == "tutorial_0") return getTutorialLevel(context)
        return getGalleryLevels(context).find { it.levelId == id }
    }
    
    fun getLevel(context: Context, id: Int): LevelConfig? {
        // Backward compatibility: c0 used to be tutorial
        if (id == 0) return getTutorialLevel(context)
        return getGalleryLevels(context).find { it.levelId == "c$id" }
    }

    fun getClassicLevels(context: Context): List<LevelConfig> {
        val gallery = getGalleryLevels(context).take(60)
        return listOf(getTutorialLevel(context)) + gallery
    }

    // --- Mode 2: Gallery (60+ Levels, Assets) ---
    // ID -> (Module, Status, Motivation, Log)
    // We added the 4th component "Log" to store the sci-fi text.
    private val storyDataMap = mutableMapOf<Int, StoryData>() 
    
    data class StoryData(
        val module: String,
        val status: String,
        val motivation: String,
        val log: String
    )

    private var cachedJsonArray: org.json.JSONArray? = null // Cache the raw JSON array
    private var cachedLanguage: String? = null // Track the language for cache invalidation

    // Helper function to get locale-aware gallery descriptions JSON path
    private fun getGalleryDescriptionsPath(context: Context): String {
        val savedLanguage = site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(context)
        return when {
            savedLanguage.startsWith("en") -> "gallery_levels/i18n/en.json"
            savedLanguage.startsWith("ja") -> "gallery_levels/i18n/ja.json"
            savedLanguage.startsWith("ko") -> "gallery_levels/i18n/ko.json"
            savedLanguage.startsWith("de") -> "gallery_levels/i18n/de.json"
            savedLanguage.startsWith("fr") -> "gallery_levels/i18n/fr.json"
            savedLanguage.startsWith("es") -> "gallery_levels/i18n/es.json"
            savedLanguage.startsWith("ru") -> "gallery_levels/i18n/ru.json"
            savedLanguage.startsWith("pt") -> "gallery_levels/i18n/pt.json"
            savedLanguage.startsWith("it") -> "gallery_levels/i18n/it.json"
            savedLanguage.startsWith("ar") -> "gallery_levels/i18n/ar.json"
            savedLanguage.startsWith("hi") -> "gallery_levels/i18n/hi.json"
            savedLanguage.startsWith("nl") -> "gallery_levels/i18n/nl.json"
            savedLanguage.startsWith("pl") -> "gallery_levels/i18n/pl.json"
            savedLanguage.startsWith("sv") -> "gallery_levels/i18n/sv.json"
            savedLanguage.startsWith("th") -> "gallery_levels/i18n/th.json"
            savedLanguage.startsWith("tr") -> "gallery_levels/i18n/tr.json"
            savedLanguage.startsWith("vi") -> "gallery_levels/i18n/vi.json"
            savedLanguage.contains("zh-TW") || savedLanguage.contains("zh-HK") || savedLanguage.contains("zh-MO") -> "gallery_levels/i18n/zh-rTW.json"
            else -> "gallery_levels/i18n/zh.json" // Default: Simplified Chinese
        }
    }

    private fun ensureStoryDataLoaded(context: Context) {
        val currentLanguage = site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(context)
        // Invalidate cache if language changed
        if (cachedJsonArray != null && cachedLanguage == currentLanguage) return // Already loaded for current language
        
        try {
            // Load Story Data from project_exodus_story.json
             val storyJson = context.assets.open("project_exodus_story.json")
                .bufferedReader()
                .use { it.readText() }
            val storyArray = org.json.JSONArray(storyJson)
            for (i in 0 until storyArray.length()) {
                val obj = storyArray.getJSONObject(i)
                val id = obj.optInt("level_id")
                val module = obj.optString("module_name_zh") 
                val status = obj.optString("status")
                val motivation = obj.optString("motivation")
                val log = obj.optString("log")
                storyDataMap[id] = StoryData(module, status, motivation, log)
            }

            // Load Gallery Config from locale-aware gallery_descriptions.json
            val galleryJsonPath = getGalleryDescriptionsPath(context)
            val galleryJson = context.assets.open(galleryJsonPath)
                .bufferedReader()
                .use { it.readText() }
            cachedJsonArray = org.json.JSONArray(galleryJson)
            cachedLanguage = currentLanguage // Track which language was loaded
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var cachedGalleryLevels: List<LevelConfig>? = null
    private var cachedGalleryLanguage: String? = null // Track language for gallery cache

    fun getGalleryLevels(context: Context): List<LevelConfig> {
        val currentLanguage = site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(context)
        // Invalidate gallery cache if language changed
        if (cachedGalleryLevels != null && cachedGalleryLanguage == currentLanguage && cachedGalleryLevels!!.isNotEmpty()) {
            return cachedGalleryLevels!!
        }
        // Reset caches for new language
        cachedGalleryLevels = null
        cachedJsonArray = null
        
        ensureStoryDataLoaded(context)
        val levels = mutableListOf<LevelConfig>()
        val jsonArray = cachedJsonArray ?: return emptyList()
        
        try {
            // Part 1: Main Levels (1-60)
            for (i in 0 until minOf(60, jsonArray.length())) {
                val obj = jsonArray.getJSONObject(i)
                val id = i + 1
                val levelId = "g_${id}_A"
                val filenameA = obj.optString("filename_a", "")
                
                // Story / Title
                val title = obj.optString("title", "Level $id")
                // CRITICAL: Use the Rich Mythological Story from gallery_descriptions
                val richStory = obj.optString("story_text", "")
                
                // Determine module/status
                val moduleName = determineModuleName(id) // Reuse helper
                val integrity = "Standard Integrity (100%)"
                val storyData = storyDataMap[id] // Used for motivation if needed, but not overriding main story
                
                val imageSource = if (filenameA.isNotEmpty()) {
                    ImageSource.Asset("gallery_levels/$filenameA")
                } else {
                     ImageSource.Resource(site.aiok.onepic.R.drawable.ic_launcher_background)
                }

                // 10-level Step Logic for Main Mode
                val (rows, cols) = when {
                    id <= 10 -> 3 to 4 // 12
                    id <= 20 -> 4 to 4 // 16
                    id <= 30 -> 4 to 5 // 20
                    id <= 40 -> 5 to 5 // 25
                    id <= 50 -> 5 to 6 // 30
                    else -> 6 to 6     // 36
                }

                levels.add(
                    LevelConfig(
                        levelId = levelId,
                        title = title,
                        imageSource = imageSource,
                        storyText = richStory, // Keep the beautiful story for Main Levels
                        moduleName = moduleName, // English
                        integrityStatus = integrity,
                        isAscended = false,
                        rows = rows, cols = cols, difficulty = when {
                            id <= 20 -> "Easy"
                            id <= 40 -> "Normal"
                            else -> "Intermediate"
                        }
                    )
                )
            }
            
            // Part 2: Ascended Levels (61-120) -> Mapped from 1-60
            for (i in 0 until minOf(60, jsonArray.length())) { // Still iterating 0..59 to get files
                val obj = jsonArray.getJSONObject(i)
                val originalId = i + 1
                val virtualId = 60 + originalId // 61..120
                val levelId = "g_${virtualId}_B_Virtual" 
                
                val filenameB = obj.optString("filename_b", "")
                val title = obj.optString("title", "Level $originalId") + " (Ascended)"
                
                // CRITICAL: Use the Sci-Fi Log from project_exodus_story for Ascended Levels
                val sciFiLog = storyDataMap[virtualId]?.log ?: "System Data Corrupted."
                
                val moduleName = determineModuleName(virtualId) // Modules 7-12
                val integrity = "Quantum Integrity (200%)"

                 val imageSource = if (filenameB.isNotEmpty()) {
                    ImageSource.Asset("gallery_levels/$filenameB")
                } else {
                     ImageSource.Resource(site.aiok.onepic.R.drawable.ic_launcher_background)
                }
                
                val (rows, cols) = when {
                    originalId <= 10 -> 4 to 5
                    originalId <= 20 -> 5 to 5
                    originalId <= 30 -> 5 to 6
                    originalId <= 40 -> 6 to 6
                    originalId <= 50 -> 6 to 7
                    else -> 7 to 7
                }
                levels.add(
                    LevelConfig(
                        levelId = levelId, // Use g_61_B_Virtual etc.
                        title = title,
                        imageSource = imageSource,
                        storyText = sciFiLog, // Use the sci-fi log for Ascended
                        moduleName = moduleName,
                        integrityStatus = integrity,
                        isAscended = true,
                        rows = rows, cols = cols, difficulty = when {
                            originalId <= 20 -> "Hard"
                            originalId <= 40 -> "Expert"
                            else -> "Master"
                        }
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedGalleryLevels = levels
        cachedGalleryLanguage = site.aiok.onepic.utils.LocaleHelper.getSavedLanguage(context)
        return levels
    }
    
    // Helper functions for Module Mapping (1-120)
    // 60 Main (1-60) + 60 Ascended (61-120)
    // Module 1: Main 1..5 + Ascended 61..65
    // Module 2: Main 6..10 + Ascended 66..70
    fun determineModuleName(id: Int): String {
        val baseId = if (id > 60) id - 60 else id
        val moduleIndex = (baseId - 1) / 5 // 0..11
        
        return when (moduleIndex) {
            0 -> "Quantum Keel"
            1 -> "Fusion Reactor Heart"
            2 -> "Neuro-Link Cockpit"
            3 -> "Cryostasis Hall"
            4 -> "The Bio-Dome"
            5 -> "Void Shields"
            6 -> "Hyper-Sensors"
            7 -> "Ion Thrusters"
            8 -> "Communication Spire"
            9 -> "Genesis Library"
            10 -> "Warp Drive"
            11 -> "The Launch Key"
            12 -> "Project Exodus: The Ascended Ark"
            else -> "Unknown Module"
        }
    }

    fun getBlueprintAsset(moduleIndex: Int): String {
        val num = String.format("%02d", moduleIndex + 1)
        return "blueprint/bp_$num.png"
    }

    fun getRenderAsset(moduleIndex: Int): String {
        val num = String.format("%02d", moduleIndex + 1)
        return if (moduleIndex < 12) "blueprint_renders/render_$num.png" else "blueprint/final_render.png"
    }

    private fun determineIntegrity(id: Int): String {
        return when (id) {
            in 1..60 -> "Standard Integrity (100%)"
            in 61..120 -> "Reinforced Integrity (200%)"
            else -> "Unknown"
        }
    }
    
    // Restoration for Classic Mode UI (LevelRow.kt compatibility)
    fun getAscendedLevel(context: Context, id: Int): LevelConfig? {
        // Special case for ID 0 (Tutorial/Origin)
        if (id == 0) {
            return LevelConfig(
                levelId = "tutorial_0_B",
                title = context.getString(site.aiok.onepic.R.string.level_tutorial_title_asc),
                difficulty = "Training+",
                imageSource = ImageSource.Asset("gallery_levels/level_00_B_TheOrigin.jpg"),
                rows = 3,
                cols = 3,
                storyText = context.getString(site.aiok.onepic.R.string.level_tutorial_story_asc),
                animationTheme = "mechanical",
                isAscended = true
            )
        }
        // Main ID is 1..60. 
        // The Ascended Partner has virtual ID 61..120.
        // We need to return valid config.
        
        val jsonArray = cachedJsonArray ?: return null
        if (id <= 0 || id > jsonArray.length()) return null
        
        val i = id - 1
        val obj = jsonArray.getJSONObject(i)
        val filenameB = obj.optString("filename_b", "")
        if (filenameB.isEmpty()) return null
        
        val title = obj.optString("title", "Level $id") + " (Ascended)"
        val virtualId = 60 + id
        val log = storyDataMap[virtualId]?.log ?: "System Ready."
        
        val moduleName = determineModuleName(virtualId) // Virtual ID logic
        
        return LevelConfig(
            levelId = "g_${id}_B", // Use B format for Classic Mode match
            title = title,
            imageSource = ImageSource.Asset("gallery_levels/$filenameB"),
            storyText = log,
            moduleName = moduleName,
            integrityStatus = "Quantum Integrity (200%)",
            isAscended = true,
            rows = 4, cols = 5, difficulty = "Hard"
        )
    }

    
    /**
     * 从文件名中提取数字，用于自然排序
     */
    private fun extractNumber(filename: String): Int? {
        val regex = Regex("""(\d+)""")
        val match = regex.find(filename)
        return match?.value?.toIntOrNull()
    }

    // --- Mode 3: Custom ---
    fun createCustomLevels(uris: List<Uri>): List<LevelConfig> {
        val levels = mutableListOf<LevelConfig>()
        val maxLevels = 10
        for (i in 0 until maxLevels) {
            if (i < uris.size) {
                 levels.add(
                    LevelConfig(
                        levelId = "cust_${i+1}",
                        title = "Custom ${i+1}",
                        difficulty = "User",
                        imageSource = ImageSource.UriSource(uris[i]),
                        rows = 3, cols = 3
                    )
                )
            }
        }
        return levels
    }
    
    fun getCustomLevelConfig(index: Int, uri: Uri): LevelConfig {
        return LevelConfig(
            levelId = "cust_${index+1}",
            title = "Custom ${index+1}",
            difficulty = "User",
            imageSource = ImageSource.UriSource(uri),
            rows = 3, cols = 3
        )
    }
}
