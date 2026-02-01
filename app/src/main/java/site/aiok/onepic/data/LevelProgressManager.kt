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
    private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_UNLOCKED_ASCENDED = "unlocked_ascended" // Set<String> of IDs (e.g. "1", "2")
    private const val KEY_COMPLETED_ASCENDED = "completed_ascended"
    private const val KEY_STARS_ASCENDED = "stars_ascended_"
    private const val KEY_LAST_PLAYED_LEVEL = "last_played_level"  // 最后玩的关卡索引
    private const val KEY_DOUBLE_COINS_END_TIME = "double_coins_end_time" // Buff 结束时间戳
    private const val KEY_BUFF_WARM_UP_END_TIME = "buff_warm_up_end_time" // 10秒预热结束时间戳
    private const val KEY_DOUBLE_COINS_READY = "double_coins_ready" // 已废弃，改为自动触发
    private const val KEY_USER_NICKNAME = "user_nickname"
    private const val KEY_USER_AVATAR_PATH = "user_avatar_path"
    private const val KEY_DAILY_REMINDER_ENABLED = "daily_reminder_enabled"
    private const val KEY_LAST_RED_DOT_DATE = "last_red_dot_date" // 上一次点击消除红点的日期
    private const val KEY_PREFERRED_REMINDER_SLOT = "preferred_reminder_slot" // 记录用户点击过的偏好时段索引
    
    // Diminishing Ad Rewards
    private const val KEY_DAILY_AD_VIEWS = "daily_ad_views"
    private const val KEY_AD_VIEWS_DATE = "ad_views_date"
    private const val KEY_LAST_AD_TIME = "last_ad_time" // Last ad view timestamp
    private const val AD_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes in milliseconds

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 获取已解锁的关卡索引集合
    fun getUnlockedClassicLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val saved = prefs.getStringSet(KEY_UNLOCKED_CLASSIC, null)
        return saved?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf(0) // 默认第一关解锁 (从0开始，包含引导关)
    }

    fun getUnlockedGalleryLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val saved = prefs.getStringSet(KEY_UNLOCKED_GALLERY, null)?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet() // 默认不解锁任何飞升关卡
        // Auto-sync: Completed Classic Levels are automatically UNLOCKED in Gallery
        val completedClassic = getCompletedClassicLevels(context)
        return saved + completedClassic
    }

    // 获取已完成的关卡索引集合
    fun getCompletedClassicLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val saved = prefs.getStringSet(KEY_COMPLETED_CLASSIC, null)
        return saved?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun getCompletedGalleryLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        val gallery = prefs.getStringSet(KEY_COMPLETED_GALLERY, null)?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        val classic = getCompletedClassicLevels(context)
        // Auto-sync: Classic Levels (Main Story) should also count as Gallery unlocked
        return gallery + classic
    }

    // 解锁关卡
    fun unlockClassicLevel(context: Context, levelIndex: Int) {
        android.util.Log.d("LevelProgress", "unlockClassicLevel called for index $levelIndex")
        val prefs = getPrefs(context)
        val current = getUnlockedClassicLevels(context).toMutableSet()
        val isNewUnlock = !current.contains(levelIndex)
        android.util.Log.d("LevelProgress", "isNewUnlock=$isNewUnlock for index $levelIndex")
        current.add(levelIndex)
        prefs.edit().putStringSet(KEY_UNLOCKED_CLASSIC, current.map { it.toString() }.toSet()).apply()
        if (isNewUnlock) {
            android.util.Log.d("LevelProgress", "Locked state updated for index: $levelIndex")
        }
    }

    fun unlockGalleryLevel(context: Context, levelIndex: Int) {
        val prefs = getPrefs(context)
        val current = getUnlockedGalleryLevels(context).toMutableSet()
        val isNewUnlock = !current.contains(levelIndex)
        current.add(levelIndex)
        prefs.edit().putStringSet(KEY_UNLOCKED_GALLERY, current.map { it.toString() }.toSet()).apply()
        if (isNewUnlock) {
            // Logic handled in UI
        }
    }

    // Ascended Progress (ID based)
    fun getUnlockedAscendedLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_UNLOCKED_ASCENDED, null)?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun unlockAscendedLevel(context: Context, levelId: Int) {
        val prefs = getPrefs(context)
        val current = getUnlockedAscendedLevels(context).toMutableSet()
        val isNewUnlock = !current.contains(levelId)
        current.add(levelId)
        prefs.edit().putStringSet(KEY_UNLOCKED_ASCENDED, current.map { it.toString() }.toSet()).apply()
        if (isNewUnlock) {
            // Logic handled in UI
        }
    }

    fun getCompletedAscendedLevels(context: Context): Set<Int> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_COMPLETED_ASCENDED, null)?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun markAscendedLevelCompleted(context: Context, levelId: Int) {
        val prefs = getPrefs(context)
        val current = getCompletedAscendedLevels(context).toMutableSet()
        current.add(levelId)
        prefs.edit().putStringSet(KEY_COMPLETED_ASCENDED, current.map { it.toString() }.toSet()).apply()
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

    fun getAscendedLevelStars(context: Context, levelId: Int): Int {
        val prefs = getPrefs(context)
        return prefs.getInt("$KEY_STARS_ASCENDED$levelId", 0)
    }

    fun saveAscendedLevelStars(context: Context, levelId: Int, stars: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt("$KEY_STARS_ASCENDED$levelId", stars).apply()
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

    fun addCoins(context: Context, amount: Int) {
        val prefs = getPrefs(context)
        val currentTotal = prefs.getInt(KEY_TOTAL_MERGE_SCORE, 0)
        // Note: Direct add doesn't trigger 2X buff logic (that's for merge gain)
        // Unless we want ads to also be 2X? Usually not.
        val newTotal = maxOf(0, currentTotal + amount)
        prefs.edit().putInt(KEY_TOTAL_MERGE_SCORE, newTotal).apply()
    }

    fun consumeCoins(context: Context, amount: Int): Boolean {
        val prefs = getPrefs(context)
        val currentTotal = prefs.getInt(KEY_TOTAL_MERGE_SCORE, 0)
        if (currentTotal >= amount) {
            prefs.edit().putInt(KEY_TOTAL_MERGE_SCORE, currentTotal - amount).apply()
            return true
        }
        return false
    }

    // 保存和获取总合并得分（游戏中合并拼图块获得的分数）
    // score可以是正数（合并得分）或负数（拆解扣分）
    fun saveTotalMergeScore(context: Context, score: Int) {
        val prefs = getPrefs(context)
        val currentTotal = prefs.getInt(KEY_TOTAL_MERGE_SCORE, 0)
        
        // 如果 Buff 激活，分数翻倍
        val finalScore = if (isDoubleCoinsActive(context)) score * 2 else score
        
        val newTotal = maxOf(0, currentTotal + finalScore)  // 确保不为负
        prefs.edit().putInt(KEY_TOTAL_MERGE_SCORE, newTotal).apply()
    }
    
    // 激活双倍金币 Buff: 先进行 10 秒预热，预热后再开始正式 Buff
    fun activateDoubleCoinsBuff(context: Context, buffDurationSeconds: Int) {
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()
        val warmUpEndTime = now + 10_000L // 10秒预热
        val buffEndTime = warmUpEndTime + (buffDurationSeconds * 1000L)
        
        prefs.edit()
            .putLong(KEY_BUFF_WARM_UP_END_TIME, warmUpEndTime)
            .putLong(KEY_DOUBLE_COINS_END_TIME, buffEndTime)
            .apply()
    }
    
    // 检查是否在预热期内
    fun isBuffWarmingUp(context: Context): Boolean {
        val prefs = getPrefs(context)
        val endTime = prefs.getLong(KEY_BUFF_WARM_UP_END_TIME, 0L)
        val now = System.currentTimeMillis()
        return now < endTime
    }
    
    // 获取预热剩余秒数
    fun getWarmUpRemainingSeconds(context: Context): Int {
        val prefs = getPrefs(context)
        val endTime = prefs.getLong(KEY_BUFF_WARM_UP_END_TIME, 0L)
        val remaining = (endTime - System.currentTimeMillis()) / 1000L
        return maxOf(0, remaining.toInt())
    }

    // 检查双倍金币 Buff 是否激活 (预热结束后)
    fun isDoubleCoinsActive(context: Context): Boolean {
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()
        val warmUpEndTime = prefs.getLong(KEY_BUFF_WARM_UP_END_TIME, 0L)
        val buffEndTime = prefs.getLong(KEY_DOUBLE_COINS_END_TIME, 0L)
        return now >= warmUpEndTime && now < buffEndTime
    }
    
    // 检查双倍金币 Buff 是否就绪（已获得但未激活）
    fun isDoubleCoinsReady(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_DOUBLE_COINS_READY, false)
    }

    // 设置 Buff 为就绪状态
    fun setDoubleCoinsReady(context: Context, ready: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_DOUBLE_COINS_READY, ready).apply()
    }

    // 计算 Buff 时长：基础 3 分钟 + 每连续签到一天增加 1 分钟
    fun calculateBuffDuration(consecutiveDays: Int): Int {
        return 180 + (consecutiveDays * 60)
    }

    // 获取 Buff 预计时长（不实际启动）
    fun getBuffEstimatedSeconds(context: Context): Int {
        val checkInPrefs = context.getSharedPreferences("check_in", Context.MODE_PRIVATE)
        val consecutiveDays = checkInPrefs.getInt("consecutive_days", 1)
        return calculateBuffDuration(consecutiveDays)
    }

    // 获取 Buff 剩余秒数
    fun getDoubleCoinsRemainingSeconds(context: Context): Int {
        val prefs = getPrefs(context)
        val endTime = prefs.getLong(KEY_DOUBLE_COINS_END_TIME, 0L)
        val remaining = (endTime - System.currentTimeMillis()) / 1000L
        return maxOf(0, remaining.toInt())
    }
    
    fun getTotalMergeScore(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_TOTAL_MERGE_SCORE, 0)
    }
    
    private const val KEY_LAST_FREE_HINT_DATE = "last_free_hint_date"

    fun resetTotalMergeScore(context: Context) {
        getPrefs(context).edit().putInt(KEY_TOTAL_MERGE_SCORE, 0).apply()
    }
    
    // Check if free hint is available (once per day)
    fun isFreeHintAvailable(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastUsed = prefs.getLong(KEY_LAST_FREE_HINT_DATE, 0L)
        val today = java.time.LocalDate.now().toEpochDay()
        return lastUsed != today
    }
    
    fun useFreeHint(context: Context) {
        val prefs = getPrefs(context)
        val today = java.time.LocalDate.now().toEpochDay()
        prefs.edit().putLong(KEY_LAST_FREE_HINT_DATE, today).apply()
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

    // 检查是否首次启动
    fun isFirstLaunch(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).commit()
    }
    
    // ========== Diminishing Ad Rewards ==========
    
    /**
     * Get today's date string (YYYY-MM-DD)
     */
    private fun getTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }
    
    /**
     * Get today's ad view count, resets daily.
     */
    fun getDailyAdViews(context: Context): Int {
        val prefs = getPrefs(context)
        val savedDate = prefs.getString(KEY_AD_VIEWS_DATE, "") ?: ""
        val today = getTodayDate()
        
        // Reset if it's a new day
        if (savedDate != today) {
            return 0
        }
        return prefs.getInt(KEY_DAILY_AD_VIEWS, 0)
    }
    
    /**
     * Increment daily ad view count.
     */
    fun incrementDailyAdViews(context: Context) {
        val prefs = getPrefs(context)
        val today = getTodayDate()
        val savedDate = prefs.getString(KEY_AD_VIEWS_DATE, "") ?: ""
        
        val newCount = if (savedDate != today) {
            1 // New day, start fresh
        } else {
            prefs.getInt(KEY_DAILY_AD_VIEWS, 0) + 1
        }
        
        prefs.edit()
            .putString(KEY_AD_VIEWS_DATE, today)
            .putInt(KEY_DAILY_AD_VIEWS, newCount)
            .apply()
    }
    
    /**
     * Calculate diminishing reward based on daily ad views.
     * First ad: 100 coins
     * Subsequent: -20 each time, minimum 10 coins
     * Pattern: 100, 80, 60, 40, 20, 10, 10, 10...
     */
    fun calculateAdReward(context: Context): Int {
        val viewCount = getDailyAdViews(context)
        return maxOf(10, 100 - (viewCount * 20))
    }
    
    /**
     * Check if ad cooldown has passed (5 minutes between views).
     */
    fun canWatchAd(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastAdTime = prefs.getLong(KEY_LAST_AD_TIME, 0L)
        return System.currentTimeMillis() - lastAdTime >= AD_COOLDOWN_MS
    }
    
    /**
     * Get remaining cooldown time in seconds.
     */
    fun getAdCooldownRemaining(context: Context): Int {
        val prefs = getPrefs(context)
        val lastAdTime = prefs.getLong(KEY_LAST_AD_TIME, 0L)
        val elapsed = System.currentTimeMillis() - lastAdTime
        val remaining = AD_COOLDOWN_MS - elapsed
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }
    
    /**
     * Record ad view timestamp (call after successful ad view).
     */
    fun recordAdView(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().putLong(KEY_LAST_AD_TIME, System.currentTimeMillis()).apply()
    }

    // ========== User Profile ==========

    fun getNickname(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_USER_NICKNAME, null) ?: "Commander"
    }

    fun saveNickname(context: Context, nickname: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_USER_NICKNAME, nickname.trim()).apply()
    }

    fun getAvatarPath(context: Context): String? {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_USER_AVATAR_PATH, null)
    }

    fun saveAvatarPath(context: Context, path: String?) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_USER_AVATAR_PATH, path).apply()
    }

    fun isDailyReminderEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        // 默认开启提醒
        return prefs.getBoolean(KEY_DAILY_REMINDER_ENABLED, true)
    }

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_DAILY_REMINDER_ENABLED, enabled).apply()
    }

    /**
     * 判断是否应该显示签到红点
     * 规则：如果今天还没点击过签到（无论是否真的点击了签到按钮，只要进入过签到页或点击了签到Tab），则显示
     */
    fun shouldShowCheckInRedDot(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastSeenDate = prefs.getString(KEY_LAST_RED_DOT_DATE, "") ?: ""
        
        // 检查今日是否已签到
        val checkInPrefs = context.getSharedPreferences("check_in", Context.MODE_PRIVATE)
        val lastCheckInDate = checkInPrefs.getString("last_check_in_date", "") ?: ""
        
        val today = java.time.LocalDate.now().toString()
        
        // 如果今天已经签到了，或者今天已经点击过消除红点了，则不显示
        return lastCheckInDate != today && lastSeenDate != today
    }

    /**
     * 标记今日红点已查看（消除红点）
     */
    fun markCheckInRedDotSeen(context: Context) {
        val prefs = getPrefs(context)
        val today = java.time.LocalDate.now().toString()
        prefs.edit().putString(KEY_LAST_RED_DOT_DATE, today).apply()
    }

    /**
     * 获取用户偏好的通知时段索引 (-1 表示未设置，由随机决定)
     */
    fun getPreferredReminderSlot(context: Context): Int {
        val prefs = getPrefs(context)
        return prefs.getInt(KEY_PREFERRED_REMINDER_SLOT, -1)
    }

    /**
     * 设置用户偏好的通知时段索引
     */
    fun setPreferredReminderSlot(context: Context, slotIndex: Int) {
        val prefs = getPrefs(context)
        prefs.edit().putInt(KEY_PREFERRED_REMINDER_SLOT, slotIndex).apply()
    }
}

