package site.aiok.onepic.data

import android.content.Context
import android.content.SharedPreferences

object LevelProgressManager {
    private const val PREFS_NAME = "level_progress"
    private const val KEY_UNLOCKED_CLASSIC = "unlocked_classic"
    private const val KEY_UNLOCKED_GALLERY = "unlocked_gallery"
    private const val KEY_COMPLETED_CLASSIC = "completed_classic"
    private const val KEY_COMPLETED_GALLERY = "completed_gallery"
    private const val KEY_STARS_CLASSIC = "stars_classic_"
    private const val KEY_STARS_GALLERY = "stars_gallery_"
    private const val KEY_BEST_TIME_CLASSIC = "best_time_classic_"
    private const val KEY_BEST_TIME_GALLERY = "best_time_gallery_"
    private const val KEY_TOTAL_MERGE_SCORE = "total_merge_score"  // 总合并得分
    private const val KEY_TOTAL_STARS = "total_stars"  // 总星星数（累计所有关卡的星星）
    private const val KEY_LAST_PLAYED_LEVEL = "last_played_level"  // 最后玩的关卡索引

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 获取已解锁的关卡索引集合
    fun getUnlockedClassicLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val saved = prefs.getStringSet(KEY_UNLOCKED_CLASSIC, null)
        return saved?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(0) // 默认第一关解锁
    }

    fun getUnlockedGalleryLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val saved = prefs.getStringSet(KEY_UNLOCKED_GALLERY, null)
        return saved?.mapNotNull { it.toIntOrNull() }?.toSet() ?: (0..9).toSet() // 默认前10关解锁
    }

    // 获取已完成的关卡索引集合
    fun getCompletedClassicLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val saved = prefs.getStringSet(KEY_COMPLETED_CLASSIC, null)
        return saved?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun getCompletedGalleryLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val saved = prefs.getStringSet(KEY_COMPLETED_GALLERY, null)
        return saved?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    // 解锁关卡
    fun unlockClassicLevel(context: Context, levelIndex: Int) {
        val prefs = getPrefs(context)
        val current = getUnlockedClassicLevels(context).toMutableSet()
        current.add(levelIndex)
        prefs.edit().putStringSet(KEY_UNLOCKED_CLASSIC, current.map { it.toString() }.toSet()).apply()
    }

    fun unlockGalleryLevel(context: Context, levelIndex: Int) {
        val prefs = getPrefs(context)
        val current = getUnlockedGalleryLevels(context).toMutableSet()
        current.add(levelIndex)
        prefs.edit().putStringSet(KEY_UNLOCKED_GALLERY, current.map { it.toString() }.toSet()).apply()
    }

    // 标记关卡为完成
    fun markClassicLevelCompleted(context: Context, levelIndex: Int) {
        val prefs = getPrefs(context)
        val current = getCompletedClassicLevels(context).toMutableSet()
        current.add(levelIndex)
        prefs.edit().putStringSet(KEY_COMPLETED_CLASSIC, current.map { it.toString() }.toSet()).apply()
    }

    fun markGalleryLevelCompleted(context: Context, levelIndex: Int) {
        val prefs = getPrefs(context)
        val current = getCompletedGalleryLevels(context).toMutableSet()
        current.add(levelIndex)
        prefs.edit().putStringSet(KEY_COMPLETED_GALLERY, current.map { it.toString() }.toSet()).apply()
    }

    // 保存星星数
    fun saveClassicLevelStars(context: Context, levelIndex: Int, stars: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt("$KEY_STARS_CLASSIC$levelIndex", stars).apply()
    }

    fun saveGalleryLevelStars(context: Context, levelIndex: Int, stars: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt("$KEY_STARS_GALLERY$levelIndex", stars).apply()
    }

    // 获取星星数
    fun getClassicLevelStars(context: Context, levelIndex: Int): Int {
        val prefs = getPrefs(context)
        return prefs.getInt("$KEY_STARS_CLASSIC$levelIndex", 0)
    }

    fun getGalleryLevelStars(context: Context, levelIndex: Int): Int {
        val prefs = getPrefs(context)
        return prefs.getInt("$KEY_STARS_GALLERY$levelIndex", 0)
    }

    // 保存和获取最佳时间
    fun saveClassicLevelBestTime(context: Context, levelIndex: Int, timeInSeconds: Int) {
        val prefs = getPrefs(context)
        val currentBest = prefs.getInt("$KEY_BEST_TIME_CLASSIC$levelIndex", Int.MAX_VALUE)
        if (timeInSeconds < currentBest) {
            prefs.edit().putInt("$KEY_BEST_TIME_CLASSIC$levelIndex", timeInSeconds).apply()
        }
    }

    fun saveGalleryLevelBestTime(context: Context, levelIndex: Int, timeInSeconds: Int) {
        val prefs = getPrefs(context)
        val currentBest = prefs.getInt("$KEY_BEST_TIME_GALLERY$levelIndex", Int.MAX_VALUE)
        if (timeInSeconds < currentBest) {
            prefs.edit().putInt("$KEY_BEST_TIME_GALLERY$levelIndex", timeInSeconds).apply()
        }
    }

    fun getClassicLevelBestTime(context: Context, levelIndex: Int): Int {
        val prefs = getPrefs(context)
        return prefs.getInt("$KEY_BEST_TIME_CLASSIC$levelIndex", Int.MAX_VALUE)
    }

    fun getGalleryLevelBestTime(context: Context, levelIndex: Int): Int {
        val prefs = getPrefs(context)
        return prefs.getInt("$KEY_BEST_TIME_GALLERY$levelIndex", Int.MAX_VALUE)
    }

    // 保存和获取总合并得分（游戏中合并拼图块获得的分数）
    // score可以是正数（合并得分）或负数（拆解扣分）
    fun saveTotalMergeScore(context: Context, score: Int) {
        val prefs = getPrefs(context)
        val currentTotal = prefs.getInt(KEY_TOTAL_MERGE_SCORE, 0)
        val newTotal = maxOf(0, currentTotal + score)  // 确保不为负
        prefs.edit().putInt(KEY_TOTAL_MERGE_SCORE, newTotal).apply()
    }
    
    fun getTotalMergeScore(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_TOTAL_MERGE_SCORE, 0)
    }
    
    fun resetTotalMergeScore(context: Context) {
        getPrefs(context).edit().putInt(KEY_TOTAL_MERGE_SCORE, 0).apply()
    }
    
    // 保存和获取总星星数（累计所有关卡的最佳星星数）
    fun saveTotalStars(context: Context, starsDelta: Int) {
        val prefs = getPrefs(context)
        val currentTotal = prefs.getInt(KEY_TOTAL_STARS, 0)
        val newTotal = maxOf(0, currentTotal + starsDelta)  // 确保不为负
        prefs.edit().putInt(KEY_TOTAL_STARS, newTotal).apply()
    }
    
    fun getTotalStars(context: Context): Int {
        // 实时计算所有已完成关卡的最佳星星数总和，确保准确性
        var total = 0
        // Classic关卡
        val classicCompleted = getCompletedClassicLevels(context)
        classicCompleted.forEach { index ->
            total += getClassicLevelStars(context, index)
        }
        // Gallery关卡
        val galleryCompleted = getCompletedGalleryLevels(context)
        galleryCompleted.forEach { index ->
            total += getGalleryLevelStars(context, index)
        }
        return total
    }
    
    // 计算总分（所有关卡的星星数之和）- 已废弃，改用getTotalStars
    @Deprecated("Use getTotalStars instead")
    fun getTotalScore(context: Context): Int {
        var total = 0
        // Classic关卡
        val classicCompleted = getCompletedClassicLevels(context)
        classicCompleted.forEach { index ->
            total += getClassicLevelStars(context, index)
        }
        // Gallery关卡
        val galleryCompleted = getCompletedGalleryLevels(context)
        galleryCompleted.forEach { index ->
            total += getGalleryLevelStars(context, index)
        }
        return total
    }

    // 保存和获取最后玩的关卡索引
    fun saveLastPlayedLevel(context: Context, levelIndex: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(KEY_LAST_PLAYED_LEVEL, levelIndex).apply()
    }
    
    fun getLastPlayedLevel(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_LAST_PLAYED_LEVEL, 0)  // 默认返回0（第一关）
    }

    // 重置进度（用于测试）
    fun resetProgress(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}

