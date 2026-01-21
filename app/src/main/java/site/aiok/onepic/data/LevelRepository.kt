package site.aiok.onepic.data

import android.content.Context
import android.net.Uri
import site.aiok.onepic.model.ImageSource
import site.aiok.onepic.model.LevelConfig

object LevelRepository {

    // --- Mode 1: Classic (1 Level, Generated - Practice/Demo) ---
    val classicLevels = listOf(
        LevelConfig("c1", "Demo", "Practice", ImageSource.Generated, 3, 3)
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
            val allFiles = context.assets.list("gallery_levels") ?: emptyArray()
            
            // Filter image files and sort them properly
            // Handle filenames with parentheses for correct sorting
            val images = allFiles
                .filter { 
                    it.endsWith(".png", ignoreCase = true) || 
                    it.endsWith(".jpg", ignoreCase = true) ||
                    it.endsWith(".jpeg", ignoreCase = true)
                }
                .sortedWith { a, b ->
                    // Extract numbers from filenames for natural sorting
                    // Handles formats like "1 (1).jpg", "1 (2).jpg", etc.
                    val numA = extractNumber(a)
                    val numB = extractNumber(b)
                    when {
                        numA != null && numB != null -> numA.compareTo(numB)
                        numA != null -> -1 // Numbers come before non-numbers
                        numB != null -> 1
                        else -> a.compareTo(b, ignoreCase = true)
                    }
                }
            
            val targetCount = 50
            
            if (images.isEmpty()) {
                // Fallback if folder is empty for testing
                return emptyList()
            }

            for (i in 1..targetCount) {
                // 图片分配策略：
                // - 如果图片数量 >= 50，直接一一对应（每张图片用一次）
                // - 如果图片数量 < 50，使用偏移算法循环使用，避免连续重复
                val imageIndex = if (images.size >= targetCount) {
                    // 正好50张或更多，直接对应（关卡1用图片1，关卡2用图片2...）
                    (i - 1) % images.size
                } else {
                    // 少于50张，使用偏移算法让分布更均匀
                    val offset = (i * 7) % images.size  // 使用质数7作为步长，确保更好的分布
                    offset
                }
                val imageFileName = images[imageIndex]
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
    
    /**
     * 从文件名中提取数字，用于自然排序
     * 支持格式：1 (1).jpg, 1 (2).jpg, image_1.jpg, 001.png 等
     */
    private fun extractNumber(filename: String): Int? {
        // 尝试提取文件名中的第一个数字序列
        val regex = Regex("""(\d+)""")
        val match = regex.find(filename)
        return match?.value?.toIntOrNull()
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
