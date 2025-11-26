package site.aiok.onepic.data

import android.content.Context
import android.net.Uri
import site.aiok.onepic.model.ImageSource
import site.aiok.onepic.model.LevelConfig

object LevelRepository {

    // --- Mode 1: Classic (5 Levels, Generated) ---
    val classicLevels = listOf(
        LevelConfig("c1", "Starter", "Easy", ImageSource.Generated, 3, 3),
        LevelConfig("c2", "Warm Up", "Easy", ImageSource.Generated, 3, 4),
        LevelConfig("c3", "Challenge", "Medium", ImageSource.Generated, 4, 5),
        LevelConfig("c4", "Master", "Hard", ImageSource.Generated, 5, 6),
        LevelConfig("c5", "Grandmaster", "Expert", ImageSource.Generated, 6, 8)
    )

    // Backwards compatibility for existing code accessing 'levels'
    // We'll map this to classicLevels for now until UI is updated
    val levels = classicLevels

    fun getLevel(id: String): LevelConfig? {
        return classicLevels.find { it.levelId == id }
    }
    
    // Helper for old Int-based ID calls if any remain (temporary)
    fun getLevel(id: Int): LevelConfig? {
        return classicLevels.find { it.levelId == "c$id" }
    }

    // --- Mode 2: Gallery (50 Levels, Assets) ---
    fun getGalleryLevels(context: Context): List<LevelConfig> {
        val levels = mutableListOf<LevelConfig>()
        try {
            // List all files in assets/gallery_levels
            // Note: This requires the folder to exist and have files
            val images = context.assets.list("gallery_levels")?.sorted() ?: emptyList()
            
            // If no images, return empty or fallback? 
            // For now, we assume user will populate this folder.
            // If < 50 images, we loop them.
            
            val targetCount = 50
            
            if (images.isEmpty()) {
                // Fallback if folder is empty for testing
                return emptyList()
            }

            for (i in 1..targetCount) {
                val imageFileName = images[(i - 1) % images.size]
                val imagePath = "gallery_levels/$imageFileName"
                
                // Difficulty Curve
                // 1-10: 3x3
                // 11-20: 3x4
                // 21-30: 4x5
                // 31-40: 5x6
                // 41-50: 6x8
                val (rows, cols, diff) = when (i) {
                    in 1..10 -> Triple(3, 3, "Easy")
                    in 11..20 -> Triple(3, 4, "Easy+")
                    in 21..30 -> Triple(4, 5, "Medium")
                    in 31..40 -> Triple(5, 6, "Hard")
                    else -> Triple(6, 8, "Expert")
                }

                levels.add(
                    LevelConfig(
                        levelId = "g_$i",
                        title = "Level $i",
                        difficulty = diff,
                        imageSource = ImageSource.Asset(imagePath),
                        rows = rows,
                        cols = cols
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return levels
    }

    // --- Mode 3: Custom (10 Slots, User Uris) ---
    fun createCustomLevels(uris: List<Uri>): List<LevelConfig> {
        val levels = mutableListOf<LevelConfig>()
        val maxLevels = 10
        
        // We create 10 slots regardless, but only fill imageSource if URI exists
        // However, the UI will likely drive this by passing only selected URIs.
        // Here we map the provided URIs to the first N slots.
        
        for (i in 0 until maxLevels) {
            val levelNum = i + 1
            val (rows, cols, diff) = when (levelNum) {
                1, 2 -> Triple(2, 3, "Tutorial")
                3, 4 -> Triple(3, 4, "Easy")
                5, 6 -> Triple(4, 5, "Medium")
                7, 8 -> Triple(5, 6, "Hard")
                else -> Triple(6, 8, "Expert")
            }

            // If we have a URI for this slot, use it.
            if (i < uris.size) {
                levels.add(
                    LevelConfig(
                        levelId = "cust_$levelNum",
                        title = "Custom $levelNum",
                        difficulty = diff,
                        imageSource = ImageSource.UriSource(uris[i]),
                        rows = rows,
                        cols = cols
                    )
                )
            }
        }
        return levels
    }
    
    // Helper to get config for a specific custom slot (used when user picks an image)
    fun getCustomLevelConfig(index: Int, uri: Uri): LevelConfig {
        val levelNum = index + 1
        val (rows, cols, diff) = when (levelNum) {
            1, 2 -> Triple(2, 3, "Tutorial")
            3, 4 -> Triple(3, 4, "Easy")
            5, 6 -> Triple(4, 5, "Medium")
            7, 8 -> Triple(5, 6, "Hard")
            else -> Triple(6, 8, "Expert")
        }
        return LevelConfig(
            levelId = "cust_$levelNum",
            title = "Custom $levelNum",
            difficulty = diff,
            imageSource = ImageSource.UriSource(uri),
            rows = rows,
            cols = cols
        )
    }
}
