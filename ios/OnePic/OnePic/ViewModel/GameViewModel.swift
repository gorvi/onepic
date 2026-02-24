import SwiftUI
import Combine

class GameViewModel: ObservableObject {
    // MARK: - Constants
    /// 使用一次提示消耗的金币数
    static let HINT_COST = 100
    /// 看激励广告获得的金币数
    static let AD_REWARD_COINS = 100

    // MARK: - State
    @Published var pieces: [PuzzlePiece] = []
    @Published var isLevelCompleted: Bool = false
    /// 通关获得的星星数
    @Published var completionStars: Int = 0
    /// 实时总金币数
    @Published var totalCoins: Int = 0
    
    /// 本局得分（通过合并获得的金币）
    @Published var sessionScore: Int = 0
    /// 兼容性：UI 目前还在使用 score，改为指向 sessionScore
    var score: Int { sessionScore }
    
    /// 本局开局时金币数（用于内部追踪，不影响显示）
    private var coinsAtGameStart: Int = 0
    
    // MARK: - Configuration
    var levelConfig: LevelConfig?
    var rows: Int = 3
    var cols: Int = 3
    
    // Grid settings
    private var viewWidth: CGFloat = 0
    private var viewHeight: CGFloat = 0
    private var cellWidth: CGFloat = 0
    private var cellHeight: CGFloat = 0
    
    // Centering Offsets & Puzzle Bounds (for drag clamping)
    @Published var boardOffsetX: CGFloat = 0
    @Published var boardOffsetY: CGFloat = 0
    @Published var boardWidth: CGFloat = 0
    @Published var boardHeight: CGFloat = 0
    
    @Published var scoreEventCount: Int = 0

    private func layoutReserve(for viewWidth: CGFloat, viewHeight: CGFloat) -> (top: CGFloat, bottom: CGFloat, horizontalPadding: CGFloat) {
        // Compact-height devices need larger reserve to avoid top/bottom UI overlap.
        if viewHeight <= 700 {
            return (top: 150, bottom: 145, horizontalPadding: 24)
        }

        // Large iPad portrait screens: shrink board and leave clear top controls area.
        if viewWidth >= 900, viewHeight >= 1000 {
            return (top: 170, bottom: 160, horizontalPadding: 44)
        }

        // General tablet screens: reserve more room than phones.
        if viewWidth >= 700 {
            return (top: 138, bottom: 132, horizontalPadding: 36)
        }

        // Phones (regular height).
        return (top: 88, bottom: 118, horizontalPadding: 28)
    }
    
    var levelManager = LevelProgressManager.shared
    
    // MARK: - Tutorial State (Android Parity)
    /// 是否处于教学模式
    @Published var isTutorialMode: Bool = false
    /// 当前教学步骤
    @Published var tutorialStep: Int = 0
    
    var dragStartLocation: CGPoint = .zero
    var draggedGroupId: Int? = nil
    var initialPiecePositions: [Int: CGPoint] = [:] // id -> (x, y) Android: dragStartStates
    var initialPieceRowCol: [Int: (row: Int, col: Int)] = [:] // id -> (row: Int, col: Int) Android: PieceState 含 row/col
    
    /// Android parity: Track score per group for unmerge deduction
    var scoredGroups: [Int: Int] = [:]
    
    /// Android parity: Track paid edges to avoid duplicate coin payout
    var paidConnections: Set<String> = []
    /// 每组已付边（edgeKey 集合），拆分时从 paidConnections 移除，以便再次合并能加回金币
    var paidEdgesByGroup: [Int: Set<String>] = [:]
    
    /// Android parity: Particle system for merge burst and win fireworks
    let particleSystem = ParticleSystem()
    
    // MARK: - Hint (Android parity: ghost flying)
    @Published var hintPieceId: Int? = nil
    @Published var hintTarget: CGPoint? = nil
    @Published var hintAnimProgress: CGFloat = 0
    private var hintTimer: Timer?
    
    var mainLevelIndexForAscended: Int? = nil
    
    /// 关卡开始时间，用于计算通关耗时并得到星星数（与 Android 一致）
    private var levelStartTime: Date?
    
    /// 本局已用时间（秒），用于拼图页倒计时/计时展示，每秒更新
    @Published var elapsedSeconds: Int = 0
    private var elapsedTimer: Timer?
    
    // MARK: - Initialization
    func loadLevel(_ config: LevelConfig, viewSize: CGSize, mainLevelIndexForAscended: Int? = nil) {
        self.levelConfig = config
        self.mainLevelIndexForAscended = mainLevelIndexForAscended
        self.isLevelCompleted = false
        
        self.coinsAtGameStart = levelManager.getCoins()
        self.sessionScore = 0
        
        // 初始化教学模式状态
        self.isTutorialMode = (config.levelId == "tutorial_0" || config.levelId == "c1")
        self.tutorialStep = 0
        
        self.completionStars = 0
        self.levelStartTime = Date()
        self.elapsedSeconds = 0
        startElapsedTimer()
        self.scoredGroups = [:]
        self.paidConnections = []
        self.paidEdgesByGroup = [:]
        particleSystem.clear()
        clearHint()
        
        self.rows = config.rows
        self.cols = config.cols
        self.viewWidth = viewSize.width
        self.viewHeight = viewSize.height
        
        // Load Image using ImageUtils
        guard let fullImage = ImageUtils.loadUIImage(source: config.imageSource) else {
            print("❌ GameViewModel: Failed to load image for level \(config.levelId)")
            return
        }
        
        // 拼图区域布局：根据屏幕高度动态预留，避免小屏操作区与拼图重叠
        let reserve = layoutReserve(for: viewWidth, viewHeight: viewHeight)
        let topBarReserve = reserve.top
        let bottomReserve = reserve.bottom
        let horizontalPadding = reserve.horizontalPadding
        let availableHeight = max(viewHeight - topBarReserve - bottomReserve, viewHeight * 0.5)
        let availableWidth = max(viewWidth - horizontalPadding * 2, viewWidth * 0.7)
        
        let imageRatio = fullImage.size.width / fullImage.size.height
        let contentRatio = availableWidth / availableHeight
        
        var boardWidth: CGFloat
        var boardHeight: CGFloat
        
        if imageRatio > contentRatio {
            boardWidth = availableWidth
            boardHeight = availableWidth / imageRatio
        } else {
            boardHeight = availableHeight
            boardWidth = availableHeight * imageRatio
        }
        
        self.cellWidth = boardWidth / CGFloat(cols)
        self.cellHeight = boardHeight / CGFloat(rows)
        
        self.boardOffsetX = (viewWidth - boardWidth) / 2
        self.boardOffsetY = topBarReserve + (availableHeight - boardHeight) / 2
        self.boardWidth = boardWidth
        self.boardHeight = boardHeight
        
        print("📏 Board: view=\(viewWidth)x\(viewHeight) board=\(boardWidth)x\(boardHeight) offset=(\(boardOffsetX),\(boardOffsetY)) cell=\(cellWidth)x\(cellHeight)")
        
        // Prepare Pieces
        self.pieces = ImageSlicer.slice(image: fullImage, rows: rows, cols: cols)
        
        print("🧩 Level Loaded. Piece Count: \(self.pieces.count)")
        
        // Correct Piece Dimensions to match View Cell Size (not Image Pixel Size)
        for i in 0..<self.pieces.count {
            self.pieces[i].width = self.cellWidth
            self.pieces[i].height = self.cellHeight
            print("  - Piece \(self.pieces[i].id): Image Size \(self.pieces[i].image.size)")
        }
        
        // Randomize
        scramblePieces()
        
        // 教学模式下自动展示第一步提示（幽灵动画）
        if isTutorialMode {
            executeHintSequence()
        }
        
        print("🧩 After Scramble:")
        for p in pieces {
            let isBottomLeft = (p.row >= rows - 2 && p.col <= 1)
            let tag = isBottomLeft ? " ⬅️ 左下区域" : ""
            print("  - Piece \(p.id) at (\(p.currentX), \(p.currentY)) [Grid: \(p.row),\(p.col)] size=\(p.width)x\(p.height) img=\(p.image.size)\(tag)")
        }
    }
    
    /// Android onSizeChanged 等效：尺寸变化时重算布局并同步位置
    func updateLayout(viewSize: CGSize) {
        guard !pieces.isEmpty else { return }
        viewWidth = viewSize.width
        viewHeight = viewSize.height
        let sliceW = pieces[0].image.size.width
        let sliceH = pieces[0].image.size.height
        let imageRatio = (sliceW * CGFloat(cols)) / (sliceH * CGFloat(rows))
        let reserve = layoutReserve(for: viewWidth, viewHeight: viewHeight)
        let topBarReserve = reserve.top
        let bottomReserve = reserve.bottom
        let horizontalPadding = reserve.horizontalPadding
        let availableHeight = max(viewHeight - topBarReserve - bottomReserve, viewHeight * 0.5)
        let availableWidth = max(viewWidth - horizontalPadding * 2, viewWidth * 0.7)
        let contentRatio = availableWidth / availableHeight
        var boardWidth: CGFloat, boardHeight: CGFloat
        if imageRatio > contentRatio {
            boardWidth = availableWidth
            boardHeight = availableWidth / imageRatio
        } else {
            boardHeight = availableHeight
            boardWidth = availableHeight * imageRatio
        }
        boardOffsetX = (viewWidth - boardWidth) / 2
        boardOffsetY = topBarReserve + (availableHeight - boardHeight) / 2
        self.boardWidth = boardWidth
        self.boardHeight = boardHeight
        cellWidth = boardWidth / CGFloat(cols)
        cellHeight = boardHeight / CGFloat(rows)
        for i in 0..<pieces.count {
            pieces[i].width = cellWidth
            pieces[i].height = cellHeight
        }
        syncPositionsFromGrid()
        
        // 关键修复：布局重算后刷新 Hint 目标，避免首帧/尺寸变更后目标框漂移
        if let hintId = hintPieceId,
           let hintPiece = pieces.first(where: { $0.id == hintId }) {
            hintTarget = computeHintTargetTopLeft(for: hintPiece)
            if isTutorialMode, let target = hintTarget {
                print("🎯 HintRelayout: piece=\(hintPiece.id) target=(\(Int(target.x)),\(Int(target.y))) piecePos=(\(Int(hintPiece.currentX)),\(Int(hintPiece.currentY)))")
            }
        }
    }
    
    // MARK: - Coordinate Conversion (Android 一致：左上角坐标系)
    
    /// 像素坐标转网格，Android: relativeX = x - puzzleBounds.left, col = round(relativeX/cellW)
    func toGrid(x: CGFloat, y: CGFloat) -> (row: Int, col: Int) {
        let relativeX = x - boardOffsetX
        let relativeY = y - boardOffsetY
        let col = Int(round(relativeX / cellWidth))
        let row = Int(round(relativeY / cellHeight))
        return (max(0, min(rows - 1, row)), max(0, min(cols - 1, col)))
    }
    
    /// 格子左上角像素坐标，Android: offsetX + col * cellW, offsetY + row * cellH
    func gridTopLeft(row: Int, col: Int) -> CGPoint {
        CGPoint(x: boardOffsetX + CGFloat(col) * cellWidth, y: boardOffsetY + CGFloat(row) * cellHeight)
    }
    
    /// Android updateLayout 等效：从 row/col 强制重算 currentX/Y，消除漂移
    func syncPositionsFromGrid() {
        for i in 0..<pieces.count {
            let tl = gridTopLeft(row: pieces[i].row, col: pieces[i].col)
            pieces[i].currentX = tl.x
            pieces[i].currentY = tl.y
            pieces[i].targetX = tl.x
            pieces[i].targetY = tl.y
        }
        objectWillChange.send()
    }

    /// 基于当前像素位置回写 row/col，并确保每个格子恰好一块
    func normalizeAllPiecesToGrid() {
        guard rows * cols == pieces.count else {
            // 非满格场景（例如教程）直接用像素回写
            for i in 0..<pieces.count {
                let (r, c) = toGrid(x: pieces[i].currentX, y: pieces[i].currentY)
                pieces[i].row = r
                pieces[i].col = c
            }
            syncPositionsFromGrid()
            logGridState(reason: "normalizeAllPiecesToGrid(partial)")
            return
        }
        
        var cellToIndices: [GridCell: [Int]] = [:]
        for i in 0..<pieces.count {
            let raw = toGrid(x: pieces[i].currentX, y: pieces[i].currentY)
            let clamped = GridCell(
                row: max(0, min(rows - 1, raw.row)),
                col: max(0, min(cols - 1, raw.col))
            )
            cellToIndices[clamped, default: []].append(i)
        }
        
        var emptyCells: [GridCell] = []
        for r in 0..<rows {
            for c in 0..<cols {
                let cell = GridCell(row: r, col: c)
                if cellToIndices[cell] == nil {
                    emptyCells.append(cell)
                }
            }
        }
        
        var assignment: [Int: GridCell] = [:]
        var extraIndices: [Int] = []
        var usedCells = Set<GridCell>()
        
        for (cell, indices) in cellToIndices {
            guard let keeper = indices.first else { continue }
            assignment[keeper] = cell
            usedCells.insert(cell)
            if indices.count > 1 {
                extraIndices.append(contentsOf: indices.dropFirst())
            }
        }
        
        // 将多余的块分配到空格，并 unmerge 避免“不相连的一大片”一起拖动
        for idx in extraIndices {
            if !emptyCells.isEmpty {
                let cell = emptyCells.removeFirst()
                assignment[idx] = cell
                usedCells.insert(cell)
                pieces[idx].groupId = pieces[idx].id
            } else {
                // 理论上不会发生；兜底找未使用格
                outer: for r in 0..<rows {
                    for c in 0..<cols {
                        let cell = GridCell(row: r, col: c)
                        if !usedCells.contains(cell) {
                            assignment[idx] = cell
                            usedCells.insert(cell)
                            break outer
                        }
                    }
                }
            }
        }
        
        // 若还有空格（意味着 extraIndices < emptyCells），使用任意块填补
        if !emptyCells.isEmpty {
            for cell in emptyCells {
                if let idx = pieces.indices.first(where: { assignment[$0] == nil }) {
                    assignment[idx] = cell
                    usedCells.insert(cell)
                }
            }
        }
        
        // 应用最终 assignment
        for i in 0..<pieces.count {
            if let cell = assignment[i] {
                pieces[i].row = cell.row
                pieces[i].col = cell.col
            } else {
                let raw = toGrid(x: pieces[i].currentX, y: pieces[i].currentY)
                pieces[i].row = max(0, min(rows - 1, raw.row))
                pieces[i].col = max(0, min(cols - 1, raw.col))
            }
        }
        
        splitDisconnectedGroups()
        syncPositionsFromGrid()
        logGridState(reason: "normalizeAllPiecesToGrid")
    }
    
    /// 将同一 group 内「网格不相邻」或「解答中不相邻」的块拆成独立组，避免「不是相邻的边也粘在一起」
    /// 仅当两块既网格相邻又解答相邻时才算连通，否则拆开
    private func splitDisconnectedGroups() {
        let groupIds = Set(pieces.map { $0.groupId })
        for gid in groupIds {
            let members = pieces.indices.filter { pieces[$0].groupId == gid }
            if members.count <= 1 { continue }
            var components: [[Int]] = []
            var visited = Set<Int>()
            func adjacent(_ a: Int, _ b: Int) -> Bool {
                let ra = pieces[a].row, ca = pieces[a].col
                let rb = pieces[b].row, cb = pieces[b].col
                let gridAdj = (ra == rb && abs(ca - cb) == 1) || (ca == cb && abs(ra - rb) == 1)
                guard gridAdj else { return false }
                let sa = pieces[a].id, sb = pieces[b].id
                let crA = sa / cols, ccA = sa % cols
                let crB = sb / cols, ccB = sb % cols
                let solutionAdj = (crA == crB && abs(ccA - ccB) == 1) || (ccA == ccB && abs(crA - crB) == 1)
                return solutionAdj
            }
            for start in members {
                if visited.contains(start) { continue }
                var comp: [Int] = []
                var queue = [start]
                visited.insert(start)
                while !queue.isEmpty {
                    let i = queue.removeFirst()
                    comp.append(i)
                    for j in members where !visited.contains(j) && adjacent(i, j) {
                        visited.insert(j)
                        queue.append(j)
                    }
                }
                components.append(comp)
            }
            if components.count > 1 {
                print("📋 splitDisconnectedGroups: 组\(gid) 拆成\(components.count)个连通块")
                // 为每个连通块分配独立 groupId（用该块内最小 piece.id），否则拆出块的 piece.id 可能等于原 groupId 导致仍同组
                for comp in components {
                    let newGroupId = comp.map { pieces[$0].id }.min()!
                    for idx in comp {
                        pieces[idx].groupId = newGroupId
                    }
                    let ids = comp.map { pieces[$0].id }
                    print("📋   连通块 ids=\(ids) -> groupId=\(newGroupId)")
                }
            }
        }
    }
    
    /// 格子中心（仅用于粒子、hint 等视觉）
    func gridCenter(row: Int, col: Int) -> CGPoint {
        let tl = gridTopLeft(row: row, col: col)
        return CGPoint(x: tl.x + cellWidth / 2, y: tl.y + cellHeight / 2)
    }
    
    /// 按触摸坐标查找最顶层拼图块，Android: x >= currentX && x <= currentX+width (左上角)
    func pieceIdAtLocation(x: CGFloat, y: CGFloat) -> Int? {
        let sorted = pieces.sorted { $0.zIndex > $1.zIndex }
        for piece in sorted {
            if x >= piece.currentX && x <= piece.currentX + piece.width &&
               y >= piece.currentY && y <= piece.currentY + piece.height {
                return piece.id
            }
        }
        return nil
    }
    
    // MARK: - Drag Handling
    
    func onDragChanged(pieceId: Int, translation: CGSize) {
        clearHint()
        // Find the group this piece belongs to
        guard let index = pieces.firstIndex(where: { $0.id == pieceId }) else { return }
        let groupID = pieces[index].groupId
        
        // If starting drag, record initial positions
        if draggedGroupId != groupID {
            draggedGroupId = groupID
            initialPiecePositions = [:]
            initialPieceRowCol = [:]
            for p in pieces where p.groupId == groupID {
                initialPiecePositions[p.id] = CGPoint(x: p.currentX, y: p.currentY)
                initialPieceRowCol[p.id] = (p.row, p.col)
            }
            bringGroupToFront(groupId: groupID)
        }
        
        // Apply translation with boundary clamping (Android parity)
        let groupPieces = pieces.filter { $0.groupId == groupID }
        var dx = translation.width
        var dy = translation.height
        
        // Clamp: Android 左上角，minX=currentX, maxX=currentX+width
        let minX = groupPieces.map { initialPiecePositions[$0.id]?.x ?? $0.currentX }.min() ?? 0
        let minY = groupPieces.map { initialPiecePositions[$0.id]?.y ?? $0.currentY }.min() ?? 0
        let maxX = groupPieces.map { (initialPiecePositions[$0.id]?.x ?? $0.currentX) + $0.width }.max() ?? 0
        let maxY = groupPieces.map { (initialPiecePositions[$0.id]?.y ?? $0.currentY) + $0.height }.max() ?? 0
        
        let puzzleLeft = boardOffsetX
        let puzzleTop = boardOffsetY
        let puzzleRight = boardOffsetX + boardWidth
        let puzzleBottom = boardOffsetY + boardHeight
        
        if minX + dx < puzzleLeft { dx = puzzleLeft - minX }
        if minY + dy < puzzleTop { dy = puzzleTop - minY }
        if maxX + dx > puzzleRight { dx = puzzleRight - maxX }
        if maxY + dy > puzzleBottom { dy = puzzleBottom - maxY }
        
        for i in 0..<pieces.count {
            if pieces[i].groupId == groupID, let initialPos = initialPiecePositions[pieces[i].id] {
                pieces[i].currentX = initialPos.x + dx
                pieces[i].currentY = initialPos.y + dy
            }
        }
        objectWillChange.send()
    }
    
    func onDragEnded(pieceId: Int) {
        handleActionUp(pieceId: pieceId)
        draggedGroupId = nil
        initialPiecePositions = [:]
        initialPieceRowCol = [:]
    }
    
    /// Android parity: Double-tap to unmerge/split a merged group
    func unmergeGroup(pieceId: Int) {
        guard let index = pieces.firstIndex(where: { $0.id == pieceId }) else { return }
        let groupId = pieces[index].groupId
        let groupPieces = pieces.filter { $0.groupId == groupId }
        
        guard groupPieces.count > 1 else { return }
        
        // Android parity: Deduct score for this group before unmerging（从金币中扣回，与分数同一来源）
        if let scoreToDeduct = scoredGroups.removeValue(forKey: groupId), scoreToDeduct > 0 {
            let currentEarned = levelManager.getCoins() - coinsAtGameStart
            let deduct = min(scoreToDeduct, max(0, currentEarned))
            if deduct > 0 { 
                levelManager.addCoins(-deduct)
                self.sessionScore = max(0, self.sessionScore - deduct)
            }
        }
        // 拆分时移除该组已付边，以便再次合并能加回金币
        if let edges = paidEdgesByGroup.removeValue(forKey: groupId) {
            paidConnections.subtract(edges)
        }
        
        for i in 0..<pieces.count where pieces[i].groupId == groupId {
            pieces[i].groupId = pieces[i].id
            let tl = gridTopLeft(row: pieces[i].row, col: pieces[i].col)
            let offsetX = (CGFloat.random(in: 0...1) - 0.5) * 6
            let offsetY = (CGFloat.random(in: 0...1) - 0.5) * 6
            pieces[i].currentX = tl.x + offsetX
            pieces[i].currentY = tl.y + offsetY
            pieces[i].zIndex += 100
        }
        // Normalize z-indices
        let sorted = pieces.sorted { $0.zIndex < $1.zIndex }
        for (i, p) in sorted.enumerated() {
            if let idx = pieces.firstIndex(where: { $0.id == p.id }) {
                pieces[idx].zIndex = i
            }
        }
        
        updateHiddenEdges()
        SoundManager.shared.playRevert()
        
        // 教学模式：双击拆卡后推进到 step 2，使提示词消失
        if isTutorialMode && tutorialStep == 1 {
            print("🎓 Tutorial: Advancing to step 2 after unmerge")
            tutorialStep = 2
        }
        
        objectWillChange.send()
    }
    
// MARK: - Hint

    /// 当前金币是否足够使用一次提示
    var canAffordHint: Bool {
        levelManager.getCoins() >= Self.HINT_COST
    }

    /// Android parity: Show hint by highlighting a misplaced piece and animating ghost to target；使用前扣 HINT_COST 金币
    func showHint() {
        if canAffordHint {
            // 金币足够：直接消耗并展示
            executeHintSequence()
        } else {
            // 金币不足：展示激励视频
            print("💰 Not enough coins for hint. Requesting Ad...")
            // 检查冷却与广告就绪
            if levelManager.canWatchAd() && AdManager.shared.isRewardedReady {
                AdManager.shared.showRewarded { [weak self] in
                    guard let self = self else { return }
                    let reward = self.levelManager.getAdRewardCoins()
                    // 1. 发放奖励
                    self.levelManager.addCoins(reward)
                    self.levelManager.recordAdView()
                    // 2. 自动消耗并展示提示 (Seamless UX)
                    self.executeHintSequence()
                    print("💰 Ad Reward for Hint: \(reward) coins added.")
                }
            } else if !levelManager.canWatchAd() {
                let remaining = levelManager.getAdCooldownRemaining()
                print("⚠️ Ad Cooldown: Please wait \(remaining)s")
            } else {
                print("⚠️ Rewarded Ad not ready yet.")
                AdManager.shared.loadRewarded()
            }
        }
    }
    
    /// 执行提示逻辑（扣费 + 动画）
    private func executeHintSequence() {
        // 教学模式下不扣除金币
        if !isTutorialMode {
            levelManager.addCoins(-Self.HINT_COST)
        }
        objectWillChange.send()

        hintTimer?.invalidate()
        hintTimer = nil
        hintPieceId = nil
        hintTarget = nil
        hintAnimProgress = 0
        
        let misplaced = pieces.filter { piece in
            let targetRow = piece.id / cols
            let targetCol = piece.id % cols
            return piece.row != targetRow || piece.col != targetCol
        }
        
        guard !misplaced.isEmpty else { return }
        
        let isTutorial = levelConfig?.levelId == "tutorial_0" || levelConfig?.levelId == "c1"
        let selection: PuzzlePiece
        if isTutorial, let piece1 = misplaced.first(where: { $0.id == 1 }) {
            selection = piece1
        } else {
            selection = misplaced.randomElement() ?? misplaced[0]
        }
        
        let targetTopLeft = computeHintTargetTopLeft(for: selection)
        
        hintPieceId = selection.id
        hintTarget = targetTopLeft
        if isTutorialMode {
            let p4 = pieces.first(where: { $0.id == 3 })
            print("🎯 HintInit: select=\(selection.id) from=(\(Int(selection.currentX)),\(Int(selection.currentY))) target=(\(Int(targetTopLeft.x)),\(Int(targetTopLeft.y))) piece4=(\(Int(p4?.currentX ?? -1)),\(Int(p4?.currentY ?? -1)))")
        }
        hintAnimProgress = 0
        SoundManager.shared.playSnap()
        
        let startTime = Date()
        hintTimer = Timer.scheduledTimer(withTimeInterval: 1.0 / 60.0, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            let elapsed = Date().timeIntervalSince(startTime)
            let t = CGFloat(elapsed.truncatingRemainder(dividingBy: 2.0) / 2.0)
            let flyProgress: CGFloat
            if t < 0.2 { flyProgress = 0 }
            else if t > 0.8 { flyProgress = 1 }
            else { flyProgress = (t - 0.2) / 0.6 }
            let eased = flyProgress * flyProgress * (3 - 2 * flyProgress)
            self.hintAnimProgress = eased
            self.objectWillChange.send()
        }
        RunLoop.main.add(hintTimer!, forMode: .common)
    }
    
    func clearHint() {
        hintTimer?.invalidate()
        hintTimer = nil
        hintPieceId = nil
        hintTarget = nil
        hintAnimProgress = 0
    }
    
    /// Android parity: 返回提示目标的左上角坐标
    private func computeHintTargetTopLeft(for selection: PuzzlePiece) -> CGPoint {
        let isTutorial = levelConfig?.levelId == "tutorial_0" || levelConfig?.levelId == "c1"
        let targetRow = selection.id / cols
        let targetCol = selection.id % cols
        
        if isTutorial, selection.id != 3, let piece4 = pieces.first(where: { $0.id == 3 }) {
            return CGPoint(x: piece4.currentX, y: piece4.currentY)
        }
        return gridTopLeft(row: targetRow, col: targetCol)
    }

    /// 看激励广告得动态奖励金币
    func requestCoinsFromAd() {
        let pm = levelManager
        guard pm.canWatchAd() else {
            let remaining = pm.getAdCooldownRemaining()
            print("⚠️ Ad Cooldown: \(remaining)s remaining")
            return
        }
        
        if pm.canWatchAd() && AdManager.shared.isRewardedReady {
            AdManager.shared.showRewarded { [weak self] in
                guard let self = self else { return }
                let reward = pm.getAdRewardCoins()
                pm.addCoins(reward)
                pm.recordAdView()
                self.objectWillChange.send()
                print("💰 Ad Reward: \(reward) coins added.")
            }
        }
    }
    
    /// 结算时看激励广告金币翻倍
    func rewardCoinsDouble() {
        AdManager.shared.showRewarded { [weak self] in
            guard let self = self else { return }
            // 翻倍奖励：再加一份当前局的分数
            let currentScore = self.sessionScore
            self.levelManager.addCoins(currentScore)
            self.objectWillChange.send()
        }
    }
    
    // MARK: - Helper Methods
    
    private func bringGroupToFront(groupId: Int) {
        // Find max zIndex
        let maxZ = pieces.map { $0.zIndex }.max() ?? 0
        for i in 0..<pieces.count {
            if pieces[i].groupId == groupId {
                pieces[i].zIndex = maxZ + 1
            }
        }
    }
    
    func checkForWin() {
        // Android Logic: Win when all pieces are merged into one group
        guard !pieces.isEmpty else { return }
        if isLevelCompleted { return }
        
        let firstGroupId = pieces[0].groupId
        let allSameGroup = pieces.allSatisfy { $0.groupId == firstGroupId }
        
        if allSameGroup {
            triggerWin()
            return
        }
        
        // 备用胜利判定（Android 无此逻辑，用于解决「合并完成但未识别」）
        // 用 toGrid(currentX,currentY) 判定实际格位，不依赖可能未同步的 row/col
        let allInCorrectPosition = pieces.allSatisfy { p in
            let (r, c) = toGrid(x: p.currentX, y: p.currentY)
            return r == p.id / cols && c == p.id % cols
        }
        if allInCorrectPosition && !particleSystem.isFireworksMode {
            // 强制统一 groupId，保持状态一致
            let targetId = pieces[0].groupId
            for i in 0..<pieces.count {
                pieces[i].groupId = targetId
            }
            objectWillChange.send()
            triggerWin()
        }
    }
    
    /// 根据通关时间和碎片数量计算星星数（与 Android LevelCompleteDialog 一致）
    static func calculateStars(timeInSeconds: Int, rows: Int, cols: Int) -> Int {
        let pieces = rows * cols
        let threeStarThreshold = pieces * 4
        let twoStarThreshold = pieces * 8
        if timeInSeconds <= threeStarThreshold { return 3 }
        if timeInSeconds <= twoStarThreshold { return 2 }
        return 1
    }
    
    /// 拼图页消失时调用，停止计时器
    func stopElapsedTimer() {
        elapsedTimer?.invalidate()
        elapsedTimer = nil
    }
    
    /// 启动关卡计时器（每秒更新 elapsedSeconds）
    func startElapsedTimer() {
        elapsedTimer?.invalidate()
        elapsedTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self, !self.isLevelCompleted else { return }
            self.elapsedSeconds = Int(Date().timeIntervalSince(self.levelStartTime ?? Date()))
        }
        if let t = elapsedTimer { RunLoop.main.add(t, forMode: .common) }
    }
    
    private func triggerWin() {
        stopElapsedTimer()
        let elapsed = Int((Date().timeIntervalSince(levelStartTime ?? Date())))
        completionStars = Self.calculateStars(timeInSeconds: elapsed, rows: rows, cols: cols)
        isLevelCompleted = true
        particleSystem.isFireworksMode = true
        if let config = levelConfig {
            let stars = completionStars
            if config.isAscended, let mainId = mainLevelIndexForAscended {
                LevelProgressManager.shared.markAscendedLevelCompleted(mainLevelId: mainId)
                let currentBest = LevelProgressManager.shared.getAscendedLevelStars(mainLevelId: mainId)
                if stars > currentBest {
                    LevelProgressManager.shared.saveAscendedLevelStars(mainLevelId: mainId, stars: stars)
                }
            } else {
                let levels = LevelRepository.shared.getClassicLevels()
                if let mainIdx = levels.firstIndex(where: { $0.levelId == config.levelId }),
                   LevelRepository.shared.getAscendedLevel(mainIndex: mainIdx) != nil {
                    LevelProgressManager.shared.pendingAscendedUnlockMainIndex = mainIdx
                }
                LevelProgressManager.shared.markLevelCompleted(config.levelId)
                let currentBest = LevelProgressManager.shared.getStars(for: config.levelId)
                if stars > currentBest {
                    LevelProgressManager.shared.saveStars(for: config.levelId, stars: stars)
                }
                if let nextId = LevelRepository.shared.getNextLevelId(after: config.levelId) {
                    LevelProgressManager.shared.unlockLevel(levelId: nextId)
                }
            }
        }
        // 金币已在每次合并时通过 addCoins(coinGain) 加过，此处不再重复加 score，否则会加双倍
        LevelProgressManager.shared.lastGameCoinScore = sessionScore  // 记录本局应得金币，用于返回时校验显示
        SoundManager.shared.playWin()
        PlatformUtils.vibrateSuccess()
    }
    
    private func scramblePieces() {
        guard let levelId = levelConfig?.levelId else { return }
        
        // Android Logic Replication for Tutorial Level (1, 4, 2, 3 Visual Order)
        if levelId == "tutorial_0" || levelId == "c1" {
            if pieces.count >= 4 {
                let presetMapping: [Int: (row: Int, col: Int)] = [
                    0: (0, 0),
                    3: (0, 1),
                    1: (1, 0),
                    2: (1, 1)
                ]
                for i in 0..<pieces.count {
                    if let targetPos = presetMapping[pieces[i].id] {
                        let tl = gridTopLeft(row: targetPos.row, col: targetPos.col)
                        pieces[i].currentX = tl.x
                        pieces[i].currentY = tl.y
                        pieces[i].targetX = tl.x
                        pieces[i].targetY = tl.y
                        pieces[i].row = targetPos.row
                        pieces[i].col = targetPos.col
                    }
                }
                return
            }
        }
        
        // General Case: Shuffle until non-solved (Android parity)
        var positions: [(row: Int, col: Int)] = []
        for r in 0..<rows {
            for c in 0..<cols {
                positions.append((r, c))
            }
        }
        
        var attempt = 0
        repeat {
            positions.shuffle()
            attempt += 1
            var isSolved = true
            for i in 0..<min(pieces.count, positions.count) {
                let targetRow = pieces[i].id / cols
                let targetCol = pieces[i].id % cols
                if positions[i].0 != targetRow || positions[i].1 != targetCol {
                    isSolved = false
                    break
                }
            }
            if !isSolved || attempt >= 100 { break }
        } while true
        
        for i in 0..<min(pieces.count, positions.count) {
            let pos = positions[i]
            let tl = gridTopLeft(row: pos.row, col: pos.col)
            pieces[i].currentX = tl.x
            pieces[i].currentY = tl.y
            pieces[i].targetX = tl.x
            pieces[i].targetY = tl.y
            pieces[i].row = pos.row
            pieces[i].col = pos.col
        }
    }
}
