import Foundation
import Combine

extension Notification.Name {
    static let levelProgressDidChange = Notification.Name("levelProgressDidChange")
}

class LevelProgressManager: ObservableObject {
    static let shared = LevelProgressManager()
    private init() {
        // 语言优先级: 用户手动设置 > 系统语言 > 英语
        if let savedLang = UserDefaults.standard.string(forKey: Self.selectedLanguageKey) {
            self.currentLanguage = savedLang
        } else {
            self.currentLanguage = Self.detectSystemLanguage()
        }
        self.coins = UserDefaults.standard.integer(forKey: coinsKey)
    }
    
    @Published var currentLanguage: String
    @Published var checkInVersion: Int = 0
    @Published var coins: Int = 0
    
    // Buff 状态订阅
    @Published var buffRemainingSeconds: Int = 0
    @Published var isBuffActive: Bool = false
    @Published var isWarmUpActive: Bool = false
    
    /// 关卡完成时暂存待播放解锁动画的下一关 ID，在用户返回 Home 时触发（参考 Android pendingUnlockedLevels）
    var pendingUnlockLevelIdForAnimation: String?
    
    /// 完成主关卡后暂存待先展示的升级关卡主关 index，分步解锁时先高亮升级角标再播放下一主关动画
    var pendingAscendedUnlockMainIndex: Int?
    
    /// 本局应得金币数（score），返回时用于校验/修正 delta 显示（避免显示多于实际得分）
    var lastGameCoinScore: Int? = nil
    
    private let unlockedLevelsKey = "unlocked_levels"
    private let levelStarsKey = "level_stars"
    private let coinsKey = "user_coins"
    private let completedAscendedKey = "completed_ascended"  // Set<Int> main level ids 0-60
    private let starsAscendedPrefix = "stars_ascended_"
    
    // Check-in keys
    private let lastCheckInDateKey = "last_check_in_date"
    private let consecutiveDaysKey = "consecutive_days"
    private let checkedInDatesKey = "checked_in_dates"
    private let checkInRedDotSeenKey = "check_in_red_dot_seen"
    private let lastCheckInRewardKey = "last_check_in_reward"
    
    // User profile keys
    private let nicknameKey = "user_nickname"
    private let userAvatarStorageKey = "user_avatar_path"
    private static let selectedLanguageKey = "selected_language"
    private let explorerIdKey = "user_explorer_id"
    private let adViewCountKey = "ad_view_count"
    private let lastAdViewDateKey = "last_ad_view_date"
    private let lastAdTimeKey = "last_ad_time"
    private let buffEndTimeKey = "double_coins_end_time"
    private let buffWarmUpEndTimeKey = "buff_warm_up_end_time"
    private let adCooldownSeconds: Double = 60
    
    // MARK: - Level Unlocking
    
    func isLevelUnlocked(levelId: String) -> Bool {
        // Tutorial is always unlocked
        if levelId == "tutorial_0" { return true }
        
        let unlocked = UserDefaults.standard.stringArray(forKey: unlockedLevelsKey) ?? []
        return unlocked.contains(levelId)
    }
    
    func unlockLevel(levelId: String) {
        var unlocked = UserDefaults.standard.stringArray(forKey: unlockedLevelsKey) ?? []
        if !unlocked.contains(levelId) {
            unlocked.append(levelId)
            UserDefaults.standard.set(unlocked, forKey: unlockedLevelsKey)
        }
    }
    
    // MARK: - Level Completion
    
    private let completedLevelsKey = "completed_levels"
    
    func isLevelCompleted(_ levelId: String) -> Bool {
        // Standardize Tutorial ID
        let normalizedId = (levelId == "tutorial") ? "tutorial_0" : levelId
        
        let completed = UserDefaults.standard.stringArray(forKey: completedLevelsKey) ?? []
        return completed.contains(normalizedId)
    }
    
    func markLevelCompleted(_ levelId: String) {
        var completed = UserDefaults.standard.stringArray(forKey: completedLevelsKey) ?? []
        if !completed.contains(levelId) {
            completed.append(levelId)
            UserDefaults.standard.set(completed, forKey: completedLevelsKey)
            
            // Auto-detect if this is an ascended level and update its specific storage
            if levelId.hasSuffix("_B") {
                let index = Int(levelId.filter { $0.isNumber }) ?? 0
                markAscendedLevelCompleted(mainLevelId: index)
            }
            
            if let nextId = LevelRepository.shared.getNextLevelId(after: levelId) {
                pendingUnlockLevelIdForAnimation = nextId
            }
            NotificationCenter.default.post(name: .levelProgressDidChange, object: nil, userInfo: ["completedLevelId": levelId])
        }
    }
    
    /// 语义化查询接口：通过索引 and 类型检查通关状态 (对齐 Android)
    func isCompleted(index: Int, isAscended: Bool) -> Bool {
        if isAscended {
            // Ascended data is stored as a Set of main level ids in completedAscendedKey
            return getCompletedAscendedLevels().contains(index)
        } else {
            // Main level ID can be "tutorial_0" or "g_{index}_A"
            if index == 0 {
                return isLevelCompleted("tutorial_0")
            } else {
                return isLevelCompleted("g_\(index)_A")
            }
        }
    }
    
    // MARK: - Stars
    
    func getStars(for levelId: String) -> Int {
        let starsMap = UserDefaults.standard.dictionary(forKey: levelStarsKey) as? [String: Int] ?? [:]
        return starsMap[levelId] ?? 0
    }
    
    func saveStars(for levelId: String, stars: Int) {
        var starsMap = UserDefaults.standard.dictionary(forKey: levelStarsKey) as? [String: Int] ?? [:]
        let current = starsMap[levelId] ?? 0
        if stars > current {
            starsMap[levelId] = stars
            UserDefaults.standard.set(starsMap, forKey: levelStarsKey)
        }
    }
    
    // MARK: - Ascended Progress (main level id 0-60)
    
    func getCompletedAscendedLevels() -> Set<Int> {
        let arr = UserDefaults.standard.stringArray(forKey: completedAscendedKey) ?? []
        return Set(arr.compactMap { Int($0) })
    }
    
    func markAscendedLevelCompleted(mainLevelId: Int) {
        var arr = UserDefaults.standard.stringArray(forKey: completedAscendedKey) ?? []
        let key = "\(mainLevelId)"
        if !arr.contains(key) {
            arr.append(key)
            UserDefaults.standard.set(arr, forKey: completedAscendedKey)
            NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
        }
    }
    
    func getAscendedLevelStars(mainLevelId: Int) -> Int {
        return UserDefaults.standard.integer(forKey: starsAscendedPrefix + "\(mainLevelId)")
    }
    
    func saveAscendedLevelStars(mainLevelId: Int, stars: Int) {
        let key = starsAscendedPrefix + "\(mainLevelId)"
        let current = UserDefaults.standard.integer(forKey: key)
        if stars > current {
            UserDefaults.standard.set(stars, forKey: key)
        }
    }
    
    // MARK: - Coins
    
    func getCoins() -> Int {
        return coins
    }
    
    func addCoins(_ amount: Int) {
        DispatchQueue.main.async {
            self.objectWillChange.send()
            
            // 如果 Buff 激活且不是通过广告获得的金币（可选，对齐 Android: saveTotalMergeScore 翻倍），则翻倍
            // Android 逻辑主要针对游戏内合并得分翻倍。
            let isDoubled = self.isDoubleCoinsActive()
            let finalAmount = isDoubled ? amount * 2 : amount
            
            print("💰 addCoins: amount=\(amount) isDoubled=\(isDoubled) finalAmount=\(finalAmount) oldCoins=\(self.coins)")
            
            self.coins += finalAmount
            UserDefaults.standard.set(self.coins, forKey: self.coinsKey)
            NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
        }
    }
    
    func consumeCoins(_ amount: Int) -> Bool {
        if coins >= amount {
            DispatchQueue.main.async {
                self.objectWillChange.send()
                self.coins -= amount
                UserDefaults.standard.set(self.coins, forKey: self.coinsKey)
                NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
            }
            return true
        }
        return false
    }
    
    // MARK: - Ad Rewards (Android Parity: Diminishing & Cooldown)
    
    func getAdRewardCoins() -> Int {
        let count = getAdViewCountToday()
        // Android 逻辑：100 -> 80 -> 60 -> 40 -> 20 -> 10 (最低)
        return max(10, 100 - (count * 20))
    }

    func getAdViewCountToday() -> Int {
        let today = getCurrentDateString()
        let lastDate = UserDefaults.standard.string(forKey: lastAdViewDateKey) ?? ""
        if lastDate != today {
            return 0
        }
        return UserDefaults.standard.integer(forKey: adViewCountKey)
    }
    
    func canWatchAd() -> Bool {
        let lastTime = UserDefaults.standard.double(forKey: lastAdTimeKey)
        let now = Date().timeIntervalSince1970
        return now - lastTime >= adCooldownSeconds
    }
    
    func getAdCooldownRemaining() -> Int {
        let lastTime = UserDefaults.standard.double(forKey: lastAdTimeKey)
        let now = Date().timeIntervalSince1970
        let elapsed = now - lastTime
        return max(0, Int(adCooldownSeconds - elapsed))
    }

    func recordAdView() {
        let today = getCurrentDateString()
        var count = getAdViewCountToday()
        count += 1
        UserDefaults.standard.set(today, forKey: lastAdViewDateKey)
        UserDefaults.standard.set(count, forKey: adViewCountKey)
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: lastAdTimeKey)
        NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
    }
    
    // MARK: - Legacy / Helper Access
    
    struct UserProgress {
        let totalCoins: Int
        let totalStars: Int
    }
    
    var progress: UserProgress {
        let coins = getCoins()
        let starsMap = UserDefaults.standard.dictionary(forKey: levelStarsKey) as? [String: Int] ?? [:]
        var totalStars = starsMap.values.reduce(0, +)
        for id in 0...60 {
            totalStars += getAscendedLevelStars(mainLevelId: id)
        }
        return UserProgress(totalCoins: coins, totalStars: totalStars)
    }

    // MARK: - Check-In Logic

    /// 获取当前格式化日期 (yyyy-MM-dd)
    func getCurrentDateString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.calendar = Calendar.current
        formatter.timeZone = TimeZone.current
        return formatter.string(from: Date())
    }

    /// 检查今日是否已签到
    func hasCheckedInToday() -> Bool {
        let lastDate = UserDefaults.standard.string(forKey: lastCheckInDateKey) ?? ""
        return lastDate == getCurrentDateString()
    }

    /// 获取连续签到天数
    func getConsecutiveDays() -> Int {
        return UserDefaults.standard.integer(forKey: consecutiveDaysKey)
    }

    /// 执行签到：返回 (本次奖励金币, 更新后的连续天数)
    func performCheckIn() -> (Int, Int) {
        let today = getCurrentDateString()
        if UserDefaults.standard.string(forKey: lastCheckInDateKey) == today {
            return (0, getConsecutiveDays())
        }
        
        // 1. 更新连续天数
        let calendar = Calendar.current
        let lastDateStr = UserDefaults.standard.string(forKey: lastCheckInDateKey) ?? ""
        var currentStreak = getConsecutiveDays()
        
        if let lastDate = dateFormatter.date(from: lastDateStr) {
            if calendar.isDateInYesterday(lastDate) {
                currentStreak += 1
            } else if !calendar.isDateInToday(lastDate) {
                currentStreak = 1
            }
        } else {
            currentStreak = 1
        }
        
        // 2. 存入签到历史
        var dates = UserDefaults.standard.stringArray(forKey: checkedInDatesKey) ?? []
        if !dates.contains(today) {
            dates.append(today)
            UserDefaults.standard.set(dates, forKey: checkedInDatesKey)
        }
        
        // 3. 计算奖励 (Android 逻辑：基础 100 + 连续天数 * 10, 最高 200)
        let reward = min(100 + (currentStreak - 1) * 10, 200)
        addCoins(reward)
        
        // 4. 保存状态
        UserDefaults.standard.set(today, forKey: lastCheckInDateKey)
        UserDefaults.standard.set(currentStreak, forKey: consecutiveDaysKey)
        UserDefaults.standard.set(reward, forKey: lastCheckInRewardKey)
        UserDefaults.standard.set(true, forKey: checkInRedDotSeenKey)
        
        checkInVersion += 1
        NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
        return (reward, currentStreak)
    }

    /// 获取今日所获奖励 (如果今日已签)
    func getCheckInRewardToday() -> Int {
        if !hasCheckedInToday() { return 0 }
        return UserDefaults.standard.integer(forKey: lastCheckInRewardKey)
    }

    /// 获取签到历史记录 (过去 30 天)
    func getCheckInHistory() -> [String] {
        return UserDefaults.standard.stringArray(forKey: checkedInDatesKey) ?? []
    }

    /// 检查是否需要显示红点
    func shouldShowCheckInRedDot() -> Bool {
        if hasCheckedInToday() { return false }
        return !UserDefaults.standard.bool(forKey: checkInRedDotSeenKey)
    }

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }

    // MARK: - User Profile

    func getNickname() -> String {
        return UserDefaults.standard.string(forKey: nicknameKey) ?? "Traveler"
    }

    func saveNickname(_ name: String) {
        UserDefaults.standard.set(name, forKey: nicknameKey)
        NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
    }

    func getAvatarPath() -> String? {
        return UserDefaults.standard.string(forKey: userAvatarStorageKey)
    }

    func saveAvatarPath(_ path: String) {
        UserDefaults.standard.set(path, forKey: userAvatarStorageKey)
        NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
    }
    
    func getExplorerId() -> Int {
        if let id = UserDefaults.standard.object(forKey: explorerIdKey) as? Int {
            return id
        }
        // Generate random 6-digit id
        let newId = Int.random(in: 100000...999999)
        UserDefaults.standard.set(newId, forKey: explorerIdKey)
        return newId
    }

    // MARK: - Language

    func getSelectedLanguage() -> String {
        return self.currentLanguage
    }

    func saveLanguage(_ code: String) {
        UserDefaults.standard.set(code, forKey: Self.selectedLanguageKey)
        self.currentLanguage = code
        LevelRepository.shared.resetCache()
        NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
    }
    
    /// 检测系统语言并映射到应用支持的语言代码
    static func detectSystemLanguage() -> String {
        // 应用支持的语言列表
        let supportedLanguages: Set<String> = [
            "en", "zh", "zh-HK", "zh-TW",
            "ru", "es", "pt", "fr", "de",
            "vi", "tr", "ko", "it",
            "ar", "th", "nl", "pl",
            "sv", "hi", "ja"
        ]
        
        // 获取系统首选语言（如 "zh-Hans-CN", "en-US", "ja-JP"）
        guard let preferredLang = Locale.preferredLanguages.first else {
            return "en"
        }
        
        // 中文特殊处理
        if preferredLang.hasPrefix("zh") {
            if preferredLang.contains("Hant") {
                // 繁体中文
                if preferredLang.contains("HK") || preferredLang.contains("MO") {
                    return "zh-HK"
                }
                return "zh-TW"
            }
            // 简体中文（zh-Hans 或其他 zh 变体）
            return "zh"
        }
        
        // 尝试匹配基础语言代码（如 "en-US" → "en"）
        let baseCode = String(preferredLang.prefix(2))
        if supportedLanguages.contains(baseCode) {
            return baseCode
        }
        
        return "en"
    }
    // MARK: - Double Coins Buff (Android Parity)
    
    func activateDoubleCoinsBuff(durationSeconds: Int) {
        let now = Date().timeIntervalSince1970
        let warmUpEnd = now + 10 // 10s 预热
        let buffEnd = warmUpEnd + Double(durationSeconds)
        
        UserDefaults.standard.set(warmUpEnd, forKey: buffWarmUpEndTimeKey)
        UserDefaults.standard.set(buffEnd, forKey: buffEndTimeKey)
        
        print("⚡️ Buff激活: now=\(now) warmUpEnd=\(warmUpEnd) buffEnd=\(buffEnd) duration=\(durationSeconds)s key1=\(buffWarmUpEndTimeKey) key2=\(buffEndTimeKey)")
        
        updateBuffState()
        NotificationCenter.default.post(name: .levelProgressDidChange, object: nil)
    }
    
    func isDoubleCoinsActive() -> Bool {
        let now = Date().timeIntervalSince1970
        let warmUpEnd = UserDefaults.standard.double(forKey: buffWarmUpEndTimeKey)
        let buffEnd = UserDefaults.standard.double(forKey: buffEndTimeKey)
        let active = now >= warmUpEnd && now < buffEnd
        // 诊断日志（仅在激活期间输出）
        if warmUpEnd > 0 || buffEnd > 0 {
            print("🔍 isDoubleCoinsActive: now=\(now) warmUpEnd=\(warmUpEnd) buffEnd=\(buffEnd) active=\(active)")
        }
        return active
    }
    
    func isBuffWarmingUp() -> Bool {
        let now = Date().timeIntervalSince1970
        let warmUpEnd = UserDefaults.standard.double(forKey: buffWarmUpEndTimeKey)
        return now < warmUpEnd && warmUpEnd > 0
    }
    
    func getDoubleCoinsRemainingSeconds() -> Int {
        let now = Date().timeIntervalSince1970
        let buffEnd = UserDefaults.standard.double(forKey: buffEndTimeKey)
        return max(0, Int(buffEnd - now))
    }
    
    func getWarmUpRemainingSeconds() -> Int {
        let now = Date().timeIntervalSince1970
        let warmUpEnd = UserDefaults.standard.double(forKey: buffWarmUpEndTimeKey)
        return max(0, Int(warmUpEnd - now))
    }
    
    func updateBuffState() {
        DispatchQueue.main.async {
            let nextBuffActive = self.isDoubleCoinsActive()
            let nextWarmUpActive = self.isBuffWarmingUp()
            let nextRemaining: Int
            if nextWarmUpActive {
                nextRemaining = self.getWarmUpRemainingSeconds()
            } else if nextBuffActive {
                nextRemaining = self.getDoubleCoinsRemainingSeconds()
            } else {
                nextRemaining = 0
            }
            
            // 仅在值真正变化时赋值，避免全局视图频繁重绘导致首页抖动
            if self.isBuffActive != nextBuffActive {
                self.isBuffActive = nextBuffActive
            }
            if self.isWarmUpActive != nextWarmUpActive {
                self.isWarmUpActive = nextWarmUpActive
            }
            if self.buffRemainingSeconds != nextRemaining {
                self.buffRemainingSeconds = nextRemaining
            }
        }
    }
}
