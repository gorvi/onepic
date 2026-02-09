import SwiftUI

struct GalaxyView: View {
    // Data: All 120 levels interleaved (Classic + Ascended)
    var levels: [LevelConfig] {
        LevelRepository.shared.getAllGalleryLevels()
    }
    @ObservedObject var levelManager = LevelProgressManager.shared
    @State private var selectedTab = 0 // 0 = Memories, 1 = Blueprint
    
    // Header States
    @State private var scrollOffset: CGFloat = 0
    
    // Animation Manager for celestial visitors (shared from MainTabView)
    @ObservedObject var visitorManager: CelestialVisitorManager
    
    // Locate callback: switches to Home tab and scrolls to level
    var onLocateLevel: ((String) -> Void)?
    var body: some View {
        NavigationStack {
            ZStack {
                // 恢复正式星空背景
                SharedGalaxyBackground(atmosphereTheme: "cosmos", visitorManager: visitorManager)
                
                VStack(spacing: 0) {
                    let isCollapsed = scrollOffset > 20 
                    
                    // Custom Header (Ascended Status) - Collapsible
                    AscendedStatusHeader(isCollapsed: isCollapsed)
                        .padding(.horizontal)
                        .padding(.top, isCollapsed ? -5 : 10) 
                        .animation(.easeInOut(duration: 0.3), value: isCollapsed)
                    
                    // Tab Switcher
                    SegmentedSwitch(selectedIndex: $selectedTab)
                        .padding(.vertical, 10)
                        .onChange(of: selectedTab) { _ in
                             // Reset scroll upon tab change via state
                             scrollOffset = 0
                        }
                    
                    // Standard ScrollView with iOS 17+ Native Geometry Callback
                    ScrollView {
                        if selectedTab == 0 {
                            // Memories List Content
                            LazyVStack(spacing: 16) {
                                ForEach(levels, id: \.levelId) { level in
                                    let isUnlocked = checkLevelUnlocked(level)
                                    let index = Int(level.levelId.filter { $0.isNumber }) ?? 0
                                    let isPlayed = LevelProgressManager.shared.isCompleted(index: index, isAscended: level.isAscended)
                                    
                                    if isUnlocked {
                                        NavigationLink(destination: GameBoardView(levelConfig: level)) {
                                            MemoryItem(level: level, isCompleted: isUnlocked, isPlayed: isPlayed, onLocate: {
                                                onLocateLevel?(level.levelId)
                                            })
                                        }
                                        .buttonStyle(PlainButtonStyle())
                                    } else {
                                        MemoryItem(level: level, isCompleted: isUnlocked, isPlayed: isPlayed, onLocate: {
                                            onLocateLevel?(level.levelId)
                                        })
                                    }
                                }
                            }
                            .padding(16)
                        } else {
                            // Blueprint Content
                            let blueprintedThemes = LevelRepository.shared.getBlueprintThemes()
                            LazyVStack(spacing: 20) {
                                ForEach(blueprintedThemes) { entry in
                                    if entry.id == 12 {
                                        VStack(spacing: 16) {
                                            let allOtherOnline = blueprintedThemes.prefix(12).allSatisfy { e in
                                                let count = getUnlockedCount(for: e)
                                                return count == 10
                                            }
                                            Text("▼")
                                                .font(.system(size: 24, weight: .black))
                                                .foregroundColor(allOtherOnline ? Color(hex: 0x00E676) : Color.white.opacity(0.1))
                                            BlueprintRow(entry: entry, completedCount: getCompletedCount(for: entry))
                                        }
                                        .padding(.vertical, 10)
                                    } else {
                                        BlueprintRow(entry: entry, completedCount: getCompletedCount(for: entry))
                                    }
                                }
                            }
                            .padding(16)
                        }
                    }
                    .contentShape(Rectangle())
                    .allowsHitTesting(true)
                    .onScrollGeometryChange(for: CGFloat.self) { geo in
                        geo.contentOffset.y
                    } action: { oldValue, newValue in
                        // 增加阈值过滤，减少微小抖动导致的过度重绘，提升滑动性能
                        if abs(scrollOffset - newValue) > 0.5 {
                            scrollOffset = newValue
                        }
                    }
                }
            }
            .coordinateSpace(name: "galaxy_outer") 
            #if os(iOS)
            .toolbar(.hidden, for: .navigationBar)
            #endif
        }
        .makeTransparentBackground() 
    }
    
    /// Helper to get COMPLETED count for a sector (User Request: Blueprint counts COMPLETED)
    private func getCompletedCount(for entry: BlueprintThemeEntry) -> Int {
        var count = 0
        for lid in entry.levelIds {
            // Main level is counted if COMPLETED
            if LevelProgressManager.shared.isLevelCompleted(lid) {
                count += 1
            }
        }
        for idxStr in entry.ascendedLevelIds {
            if let idx = Int(idxStr) {
                // Ascended level is counted if COMPLETED
                if LevelProgressManager.shared.isCompleted(index: idx, isAscended: true) {
                    count += 1
                }
            }
        }
        return count
    }

    /// Helper to get unlocked count for a sector (User Request: Count unlocked, not just completed)
    private func getUnlockedCount(for entry: BlueprintThemeEntry) -> Int {
        var count = 0
        for lid in entry.levelIds {
            let index = Int(lid.filter { $0.isNumber }) ?? 0
            // Main level is counted if unlocked
            if index == 1 || LevelProgressManager.shared.isLevelUnlocked(levelId: lid) {
                count += 1
            }
        }
        for idxStr in entry.ascendedLevelIds {
            if let idx = Int(idxStr) {
                // Ascended level is unlocked if its Main level is completed
                if LevelProgressManager.shared.isCompleted(index: idx, isAscended: false) {
                    count += 1
                }
            }
        }
        return count
    }

    /// Check if a level is unlocked (Available to play/view)
    private func checkLevelUnlocked(_ level: LevelConfig) -> Bool {
        let index: Int
        if level.levelId.contains("tutorial") {
             index = 0
        } else {
             index = Int(level.levelId.filter { $0.isNumber }) ?? 0
        }
        
        if level.isAscended {
            // Ascended levels match corresponding Main level completion
            return LevelProgressManager.shared.isCompleted(index: index, isAscended: false)
        } else {
            // Level 1 is always unlocked by default if not strictly handled
            if index == 1 { return true }
            return LevelProgressManager.shared.isLevelUnlocked(levelId: level.levelId)
        }
    }
}

// MARK: - Components

struct AscendedStatusHeader: View {
    var isCollapsed: Bool = false
    
    let classicLevels = LevelRepository.shared.getClassicLevels()
    
    var completedClassicCount: Int {
        classicLevels
            .filter { !$0.levelId.contains("tutorial") }
            .filter { LevelProgressManager.shared.isLevelCompleted($0.levelId) }
            .count
    }
    
    var completedAscendedCount: Int {
        // Ascended levels are unlocked if corresponding Main level is completed
        // Wait, User wants COMPLETED Ascended count? Or Unlocked Ascended?
        // "Memory top statistics ... is 1".
        // If 1 passed, 1 completed.
        // So Ascended Completed should be counted properly.
        classicLevels
            .filter { !$0.levelId.contains("tutorial") }
            .filter { 
                 let index = Int($0.levelId.filter { $0.isNumber }) ?? 0
                 return LevelProgressManager.shared.isCompleted(index: index, isAscended: true)
            }.count
    }
    
    var completedCount: Int {
        completedClassicCount + completedAscendedCount
    }
    
    var totalCount: Int { 120 }
    
    var completionRate: Int {
        guard completedCount > 0 else { return 0 }
        return Int((Double(completedCount) / Double(totalCount)) * 100.0)
    }
    
    var body: some View {
        VStack(spacing: isCollapsed ? 4 : 8) {
            // Android Style Title
            Text(TRANS.get("asc_project_title", "PROJECT ASCENSION"))
                .font(.system(size: isCollapsed ? 10 : 12, weight: .black))
                .foregroundColor(Color(hex: 0x2979FF))
                .tracking(2)
            
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text("\(completionRate)%")
                    .font(.system(size: isCollapsed ? 28 : 48, weight: .black))
                    .foregroundColor(.white)
                
                if isCollapsed {
                    Text("(\(completedCount)/\(totalCount))")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(.white.opacity(0.6))
                        .padding(.bottom, 4)
                }
            }
            
            if !isCollapsed {
                VStack(spacing: 8) {
                    // Progress Bar
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.white.opacity(0.05))
                            .frame(height: 8)
                        
                        Capsule()
                            .fill(LinearGradient(colors: [Color(hex: 0x2979FF), Color(hex: 0x00E676)], startPoint: .leading, endPoint: .trailing))
                            .frame(width: (300 * CGFloat(completionRate) / 100.0), height: 8)
                    }
                    .frame(maxWidth: .infinity)
                    
                    HStack {
                        Text(TRANS.get("asc_status_synthesizing", "SYNTHESIZING MEMORIES..."))
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(completionRate == 100 ? Color(hex: 0x00E676) : .white.opacity(0.4))
                        
                        Spacer()
                        
                        Text("\(completedCount)/\(totalCount)")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white.opacity(0.6))
                    }
                }
                .transition(.asymmetric(insertion: .opacity.combined(with: .scale(scale: 0.9)), removal: .opacity))
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, isCollapsed ? 8 : 16)
        .onAppear {
            let completedClassic = classicLevels
                .filter { !$0.levelId.contains("tutorial") }
                .filter { LevelProgressManager.shared.isLevelCompleted($0.levelId) }
                .map { $0.levelId }
            
            let completedAscended = classicLevels.filter { 
                 let index = Int($0.levelId.filter { $0.isNumber }) ?? 0
                 return LevelProgressManager.shared.isCompleted(index: index, isAscended: true)
            }.map { $0.levelId + "_Ascended" }
            
            print("🔍 [DEBUG] Completed Classic: \(completedClassic)")
            print("🔍 [DEBUG] Completed Ascended: \(completedAscended)")
            print("🔍 [DEBUG] Total Count: \(completedClassic.count + completedAscended.count)")
        }
    }
}

// MARK: - Native Helpers (Legacy Trackers Removed)

struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

struct SegmentedSwitch: View {
    @Binding var selectedIndex: Int
    let items = [TRANS.get("asc_tab_memories", "MEMORIES"), TRANS.get("asc_tab_blueprint", "BLUEPRINT")]
    
    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<items.count, id: \.self) { index in
                Button(action: {
                    withAnimation {
                        selectedIndex = index
                    }
                }) {
                    Text(items[index])
                        .font(.system(size: 14, weight: selectedIndex == index ? .bold : .medium))
                        .foregroundColor(selectedIndex == index ? .white : .white.opacity(0.5))
                        .padding(.vertical, 8)
                        .padding(.horizontal, 24)
                        .background(
                            Capsule()
                                .fill(selectedIndex == index ? Color(hex: 0x2979FF).opacity(0.3) : Color.clear)
                        )
                        .overlay(
                            Capsule()
                                .stroke(selectedIndex == index ? Color(hex: 0x2979FF).opacity(0.5) : Color.clear, lineWidth: 1)
                        )
                }
            }
        }
        .padding(4)
        .background(Color.white.opacity(0.1))
        .clipShape(Capsule())
        .overlay(
            Capsule().stroke(Color.white.opacity(0.1), lineWidth: 1)
        )
    }
}

struct MemoryItem: View {
    let level: LevelConfig
    let isCompleted: Bool
    let isPlayed: Bool
    let onLocate: () -> Void
    
    @State private var showImagePreview = false
    
    var body: some View {
        HStack {
            imageSection
            textSection
                .padding(.leading, 12)
            
            Spacer()
            
            locateButton
        }
        .padding(12)
        .background(Color.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(Color.white.opacity(0.1), lineWidth: 0.5)
        )
        .fullScreenCover(isPresented: $showImagePreview) {
            ImagePreviewOverlay(
                image: resolveImage(), 
                title: level.title,
                description: level.storyText ?? TRANS.get("asc_memory_desc", "Unlock to read the lore."),
                onDismiss: { showImagePreview = false }
            )
        }
    }

    private var imageSection: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.black.opacity(0.3))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.white.opacity(0.1), lineWidth: 1)
                )
            
            if isCompleted {
                if let img = resolveImage() {
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 80, height: 80)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                } else {
                    // Placeholder
                     Rectangle()
                         .fill(Color.gray.opacity(0.3))
                         .frame(width: 80, height: 80)
                         .overlay(Text("?").foregroundColor(.white))
                         .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            } else {
                Image(systemName: "lock.fill")
                    .foregroundColor(Color.white.opacity(0.3))
                    .font(.system(size: 24))
            }
        }
        .frame(width: 80, height: 80)
        .onTapGesture {
            if isCompleted {
                showImagePreview = true
            }
        }
        .overlay(
            Group {
                if isCompleted && !isPlayed {
                    Text("NEW")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 2)
                        .background(Color.red)
                        .clipShape(Capsule())
                        .offset(x: -4, y: 4)
                }
            },
            alignment: .topLeading
        )
    }

    private var textSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(isCompleted ? level.title : TRANS.get("asc_memory_title", "Encoded Memory")) 
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)
                .lineLimit(1)
            
            Text(isCompleted ? (level.storyText ?? "No data logs.") : TRANS.get("asc_memory_desc", "Unlock to read the lore."))
                .font(.system(size: 12))
                .foregroundColor(.white.opacity(0.5))
                .lineLimit(2)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var locateButton: some View {
        Button(action: onLocate) {
            ZStack {
                Circle()
                    .fill(isCompleted ? Color(hex: 0x2979FF).opacity(0.2) : Color.white.opacity(0.05))
                    .frame(width: 40, height: 40)
                
                Image(systemName: "location.fill")
                    .font(.system(size: 18))
                    .foregroundColor(isCompleted ? Color(hex: 0x2979FF) : Color.white.opacity(0.3))
            }
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(!isCompleted)
    }
    
    private func resolveImage() -> UIImage? {
        if case .asset(let path) = level.imageSource {
            let filename = (path as NSString).lastPathComponent
            let nameWithoutExt = (filename as NSString).deletingPathExtension
            
            // 0. Check Cache First
            if let cached = ImageCache.shared.object(forKey: nameWithoutExt as NSString) {
                return cached
            }
            
            var loadedImage: UIImage?
            
            // 1. Try Asset Catalog
            if let assetImg = UIImage(named: nameWithoutExt) {
                loadedImage = assetImg
            }
            // 2. Try directly from Bundle Resources/gallery_levels/
            else if let webpPath = Bundle.main.path(forResource: nameWithoutExt, ofType: "webp", inDirectory: "gallery_levels") {
                loadedImage = UIImage(contentsOfFile: webpPath)
            }
            // 3. Try top-level bundle
            else if let fallbackPath = Bundle.main.path(forResource: nameWithoutExt, ofType: "webp") {
                loadedImage = UIImage(contentsOfFile: fallbackPath)
            }
            
            // Save to Cache
            if let img = loadedImage {
                ImageCache.shared.setObject(img, forKey: nameWithoutExt as NSString)
            }
            
            return loadedImage
        }
        return nil
    }
}

// Simple In-Memory Cache
class ImageCache {
    static let shared = NSCache<NSString, UIImage>()
}

// MARK: - Blueprint Components

struct BlueprintRow: View {
    let entry: BlueprintThemeEntry
    let completedCount: Int // User Request: Count COMPLETED levels
    
    var totalCount: Int { entry.id == 12 ? 0 : 10 }
    
    var isOnline: Bool {
        if entry.id == 12 { return false }
        return completedCount == 10
    }
    
    var completionRate: Double {
        guard totalCount > 0 else { return 0 }
        return Double(completedCount) / Double(totalCount)
    }

    private let macaronColors: [Color] = [
        Color(hex: 0xFFB2EBF2), // 马卡龙蓝
        Color(hex: 0xFFFFC1CC), // 樱花粉
        Color(hex: 0xFFB2FBDA), // 薄荷绿
        Color(hex: 0xFFFFF176)  // 柠檬黄
    ]

    var body: some View {
        VStack(spacing: 16) {
            // Header Info
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.themeName.uppercased())
                        .font(.system(size: 14, weight: .black))
                        .foregroundColor(.white)
                        .tracking(1)
                    
                    Text("\(TRANS.get("module_id_label", "MODULE_ID")): 0X\(String(format: "%02X", entry.id))")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(isOnline ? Color(hex: 0x00E676) : Color(hex: 0x2979FF))
                }
                Spacer()
                if isOnline {
                    StatusBadge(isOnline: true)
                }
            }

            // Android Style Double Image Layout
            HStack(spacing: 20) {
                ModuleSmallCard(
                    assetPath: LevelRepository.getBlueprintAsset(moduleIndex: entry.id),
                    isActive: completedCount > 0, 
                    isOnline: isOnline,
                    isRender: false
                )
                
                // 2. Center Progress Info
                VStack(spacing: 4) {
                    Text("\(completedCount)/10")
                        .font(.system(size: 12, weight: .black))
                        .foregroundColor(.white)
                    
                    Text("➔")
                        .font(.system(size: 16))
                        .foregroundColor(isOnline ? Color(hex: 0x00E676) : Color.white.opacity(0.1))
                    
                    // Macaron Dots
                    VStack(spacing: 4) {
                        HStack(spacing: 4) {
                            ForEach(0..<entry.levelIds.count, id: \.self) { i in
                                let lid = entry.levelIds[i]
                                let idx = Int(lid.filter { $0.isNumber }) ?? 0
                                let isDone = LevelProgressManager.shared.isCompleted(index: idx, isAscended: false)
                                Circle()
                                    .fill(isDone ? macaronColors[i % macaronColors.count] : Color.white.opacity(0.15))
                                    .frame(width: 5, height: 5)
                            }
                        }
                        HStack(spacing: 4) {
                            ForEach(0..<entry.ascendedLevelIds.count, id: \.self) { i in
                                let idxStr = entry.ascendedLevelIds[i]
                                let isDone = Int(idxStr).map { LevelProgressManager.shared.isCompleted(index: $0, isAscended: true) } ?? false
                                Circle()
                                    .fill(isDone ? macaronColors[(i+1) % macaronColors.count] : Color.white.opacity(0.15))
                                    .frame(width: 5, height: 5)
                            }
                        }
                    }
                }
                .frame(width: 60)

                // 3. Final Render Card
                ModuleSmallCard(
                    assetPath: LevelRepository.getRenderAsset(moduleIndex: entry.id),
                    isActive: isOnline,
                    isOnline: isOnline,
                    isRender: true
                )
            }
            .padding(.vertical, 8)
            
            // Progress Bar (Bottom)
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(Color.white.opacity(0.05))
                    .frame(height: 4)
                
                Capsule()
                    .fill(isOnline ? Color(hex: 0x00E676) : Color(hex: 0x2979FF))
                    .frame(width: CGFloat(completionRate) * 200, height: 4) // Simplified width calculation
            }
            .frame(maxWidth: .infinity)
        }
        .padding(20)
        .background(
            ZStack {
                RoundedRectangle(cornerRadius: 26)
                    .fill(Color.white.opacity(0.06))
                
                if isOnline {
                    LinearGradient(colors: [macaronColors[0].opacity(0.08), .clear], startPoint: .topLeading, endPoint: .bottomTrailing)
                        .clipShape(RoundedRectangle(cornerRadius: 26))
                }
            }
        )
        .overlay(
            RoundedRectangle(cornerRadius: 26)
                .stroke(isOnline ? Color(hex: 0x00E676).opacity(0.3) : Color.white.opacity(0.08), lineWidth: 1)
        )
    }
}

// MARK: - Refined Blueprint Components

struct StatusBadge: View {
    let isOnline: Bool
    var body: some View {
        Text(isOnline ? "ONLINE" : "CONSTRUCTING")
            .font(.system(size: 8, weight: .black))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(isOnline ? Color(hex: 0x00E676).opacity(0.2) : Color.white.opacity(0.1))
            .foregroundColor(isOnline ? Color(hex: 0x00E676) : Color.white.opacity(0.4))
            .clipShape(Capsule())
            .overlay(Capsule().stroke(isOnline ? Color(hex: 0x00E676).opacity(0.3) : .clear, lineWidth: 0.5))
    }
}

struct ModuleSmallCard: View {
    let assetPath: String
    let isActive: Bool
    let isOnline: Bool
    let isRender: Bool
    
    @State private var showPreview = false
    
    var body: some View {
        Button(action: {
            if isActive { showPreview = true }
        }) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(isActive ? Color.black.opacity(0.4) : Color.white.opacity(0.05))
                    .shadow(color: isOnline && isRender ? Color(hex: 0x00E676).opacity(0.4) : .clear, radius: 8)
                
                if let img = loadResourceImage(path: assetPath) {
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: 70, height: 70)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                        .opacity(isActive ? 1.0 : 0.15)
                        .grayscale(isActive ? 0 : 1.0)
                }
                
                if !isActive {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 16))
                        .foregroundColor(Color.white.opacity(0.2))
                }
                
                // Border
                RoundedRectangle(cornerRadius: 14)
                    .stroke(
                        isActive 
                        ? (isRender && isOnline ? Color(hex: 0x00E676).opacity(0.5) : Color(hex: 0x2979FF).opacity(0.3))
                        : Color.white.opacity(0.1),
                        lineWidth: isOnline && isRender ? 2 : 1
                    )
            }
        }
        .buttonStyle(PlainButtonStyle())
        .frame(width: 70, height: 70)
        .fullScreenCover(isPresented: $showPreview) {
            ImagePreviewOverlay(image: loadResourceImage(path: assetPath), onDismiss: { showPreview = false })
        }
    }
    
    private func loadResourceImage(path: String) -> UIImage? {
        // 1. Try Asset Catalog first (no extension)
        let components = path.components(separatedBy: "/")
        let filename = components.last ?? ""
        if let img = UIImage(named: filename) { return img }
        
        // 2. Try Bundle lookup with specific folder structure
        let directory = components.dropLast().joined(separator: "/")
        
        // Log for debugging (if needed in real device log)
        // print("🔍 Attempting to load: \(filename) in dir: \(directory)")
        
        if let webpPath = Bundle.main.path(forResource: filename, ofType: "webp", inDirectory: directory) {
            return UIImage(contentsOfFile: webpPath)
        }
        
        // 3. Try under "Resources" prefix if above fails (Xcode folder structure vs Bundle structure)
        if let resPath = Bundle.main.path(forResource: filename, ofType: "webp", inDirectory: "Resources/\(directory)") {
            return UIImage(contentsOfFile: resPath)
        }

        // 4. Flattened lookup fallback (in case folders aren't preserved in Bundle)
        if let flatPath = Bundle.main.path(forResource: filename, ofType: "webp") {
            return UIImage(contentsOfFile: flatPath)
        }
        
        return nil
    }
}

/// 增加科幻感的扫描线覆盖层
struct BlueprintScannerOverlay: View {
    var body: some View {
        ZStack {
            // 网格线
            Path { path in
                for i in stride(from: 0, to: 100, by: 15) {
                    path.move(to: CGPoint(x: CGFloat(i), y: 0))
                    path.addLine(to: CGPoint(x: CGFloat(i), y: 100))
                    path.move(to: CGPoint(x: 0, y: CGFloat(i)))
                    path.addLine(to: CGPoint(x: 100, y: CGFloat(i)))
                }
            }
            .stroke(Color.cyan.opacity(0.15), lineWidth: 0.5)
            
            // 微妙渐变遮罩
            RadialGradient(colors: [.clear, .black.opacity(0.3)], center: .center, startRadius: 20, endRadius: 60)
        }
    }
}

// Data model for grouping
struct BlueprintThemeEntry: Identifiable {
    let id: Int // 0..12
    let themeName: String
    let levelIds: [String]
    let ascendedLevelIds: [String]
    
    var allIds: [String] {
        levelIds + ascendedLevelIds
    }
}

extension LevelRepository {
    func getBlueprintThemes() -> [BlueprintThemeEntry] {
        var entries: [BlueprintThemeEntry] = []
        
        
        for i in 0..<12 {
            let start = i * 5 + 1
            let end = start + 4
            let mainIds = (start...end).map { "g_\($0)_A" }
            let ascendedIdxStrings = (start...end).map { "\($0)" }
            
            // Use standardized chapter names from repository
            let themeName = LevelRepository.shared.getChapterName(chapter: i + 1)
            
            entries.append(BlueprintThemeEntry(
                id: i,
                themeName: themeName,
                levelIds: mainIds,
                ascendedLevelIds: ascendedIdxStrings
            ))
        }
        
        entries.append(BlueprintThemeEntry(
            id: 12,
            themeName: "The Ark: Synthesis Key",
            levelIds: ["g_60_A"],
            ascendedLevelIds: []
        ))
        
        return entries
    }
    
    // Android Parity: Asset Path Helpers (Aligned with iOS Resources Folder)
    static func getBlueprintAsset(moduleIndex: Int) -> String {
        let num = String(format: "%02d", moduleIndex + 1)
        return "blueprint/bp_\(num)"
    }

    static func getRenderAsset(moduleIndex: Int) -> String {
        let num = String(format: "%02d", moduleIndex + 1)
        // Adjust for Final Ark (index 12)
        if moduleIndex < 12 {
            return "blueprint_renders/render_\(num)"
        } else {
            return "blueprint/bp_13" // Fallback to bp_13 for Ark if specific render not found
        }
    }
}

// MARK: - Image Preview Overlay
struct ImagePreviewOverlay: View {
    let image: UIImage?
    var title: String? = nil
    var description: String? = nil
    let onDismiss: () -> Void
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.95)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }
            
            if let img = image {
                Image(uiImage: img)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .onTapGesture { onDismiss() }
            } else {
                Text(TRANS.get("image_not_available", "Image not available"))
                    .foregroundColor(.white.opacity(0.5))
            }
            
            // Text Overlay at Bottom
            if let title = title, let description = description {
                VStack(alignment: .leading, spacing: 8) {
                    Spacer()
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(title)
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.white)
                        
                        ScrollView {
                            Text(description)
                                .font(.system(size: 14))
                                .foregroundColor(.white.opacity(0.8))
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxHeight: 150)
                    }
                    .padding(24)
                    .background(
                        LinearGradient(colors: [.black.opacity(0.8), .clear], startPoint: .bottom, endPoint: .top)
                            .ignoresSafeArea()
                    )
                }
                .allowsHitTesting(false) // Let taps pass through to dismiss (mostly)
            }
            
            // Close Button
            VStack {
                HStack {
                    Spacer()
                    Button(action: onDismiss) {
                        Image(systemName: "xmark")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 44, height: 44)
                            .background(Color.black.opacity(0.3))
                            .clipShape(Circle())
                    }
                    .padding(.top, 60)
                    .padding(.trailing, 20)
                }
                Spacer()
            }
        }
    }
}

// Wrapper for VStack compatible "Column" naming from Android if preferred, or just use VStack
typealias Column = VStack
