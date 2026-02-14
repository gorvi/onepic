import SwiftUI
import Combine

// MARK: - Helper Structures
struct GridCell: Hashable, Equatable {
    let row: Int
    let col: Int
}

extension GameViewModel {
    
    /// 更新所有碎片的隱藏邊狀態。
    /// 當兩個碎片屬於同一個 groupId 且在解答網格中物理相鄰時，隱藏它們之間的邊。
    func updateHiddenEdges() {
        for i in 0..<pieces.count {
            var hidden = Set<PuzzlePiece.Edge>()
            let p = pieces[i]
            
            // 遍歷所有其他碎片，尋找同組且相鄰的
            for other in pieces where other.groupId == p.groupId && other.id != p.id {
                // 解答中的位置
                let pr = p.id / cols, pc = p.id % cols
                let or = other.id / cols, oc = other.id % cols
                
                // 檢查四個方向
                if pr == or && pc == oc - 1 { hidden.insert(.right) }
                if pr == or && pc == oc + 1 { hidden.insert(.left) }
                if pr == or + 1 && pc == oc { hidden.insert(.top) }
                if pr == or - 1 && pc == oc { hidden.insert(.bottom) }
            }
            pieces[i].hiddenEdges = hidden
        }
    }
    
    // MARK: - Debug Logging
    
    /// 输出当前所有组、组内图片、行列、坐标等详细状态
    private func logInteractionState(prefix: String = "📋") {
        let groupIds = Set(pieces.map { $0.groupId })
        print("\(prefix) === 全局状态 rows=\(rows) cols=\(cols) 共\(pieces.count)块 ===")
        for gid in groupIds.sorted() {
            let members = pieces.filter { $0.groupId == gid }
            let ids = members.map { $0.id }.sorted()
            let cells = members.map { "\($0.id):(\($0.row),\($0.col))" }.joined(separator: ",")
            let coords = members.map { "\($0.id):(\(Int($0.currentX)),\(Int($0.currentY)))" }.joined(separator: ",")
            print("\(prefix) 组\(gid) 有\(members.count)块 ids=[\(ids)] 格位=[\(cells)] 像素=[\(coords)]")
        }
    }
    
    /// 输出移动组详情
    private func logMovedGroup(_ group: [PuzzlePiece], originCells: [GridCell], intendedCells: [GridCell], prefix: String = "📋") {
        let ids = group.map { $0.id }.sorted()
        let originStr = originCells.map { "(\($0.row),\($0.col))" }.joined(separator: ",")
        let intendedStr = intendedCells.map { "(\($0.row),\($0.col))" }.joined(separator: ",")
        let coords = group.map { "\($0.id):(\(Int($0.currentX)),\(Int($0.currentY)))" }.joined(separator: ",")
        print("\(prefix) 移动组 groupId=\(group[0].groupId) ids=[\(ids)] 块数=\(group.count)")
        print("\(prefix)   起点格位=\(originStr) 放手目标格位=\(intendedStr)")
        print("\(prefix)   当前像素坐标=\(coords)")
    }
    
    // MARK: - Core Interaction Logic
    
    /// Main entry point for drag release logic
    /// Strategy:
    /// 1. Try to SNAP to neighbors (highest priority for puzzle building)
    /// 2. Check if intended Grid Position is FREE (Priority over Push/Swap)
    /// 3. Try to INTERACT with overlapped pieces (Push or Swap)
    /// 4. If no interaction, Try to SNAP to Grid (clean placement)
    /// 5. If invalid placement, REVERT to start position
    func handleActionUp(pieceId: Int) {
        guard let index = pieces.firstIndex(where: { $0.id == pieceId }) else { return }
        let leadPiece = pieces[index]
        let groupID = leadPiece.groupId
        let movedGroup = pieces.filter { $0.groupId == groupID }
        
        logInteractionState(prefix: "📋")
        print("🖱 handleActionUp: 拖动块id=\(pieceId) 序号=\(index) groupId=\(groupID) 组内块数=\(movedGroup.count)")
        for p in movedGroup {
            print("🖱   块\(p.id) 行列=(\(p.row),\(p.col)) 像素=(\(p.currentX),\(p.currentY))")
        }
        
        // 1. Try Neighbor Snapping (Merge)
        // 合并后已同步 row/col，可安全 sync 以保持格位一致
        if checkNeighborSnapping(movedPiece: leadPiece) {
            print("📋 合并成功 归一化后:")
            SoundManager.shared.playSnap()
            PlatformUtils.vibrateSuccess()
            normalizeAllPiecesToGrid()
            updateHiddenEdges()
            logInteractionState(prefix: "📋 合并后")
            
            // Tutorial Check: Any successful move in tutorial mode advances to step 1
            if isTutorialMode && tutorialStep == 0 {
                print("🎓 Tutorial: Advancing to step 1")
                tutorialStep = 1
            }
            
            checkForWin()
            return
        }
        
        // Android: dragDx = movedMinX - groupOriginMinX (左上角)
        let movedMinX = movedGroup.map { $0.currentX }.min() ?? 0
        let movedMinY = movedGroup.map { $0.currentY }.min() ?? 0
        let groupOriginMinX = movedGroup.compactMap { initialPiecePositions[$0.id]?.x }.min() ?? leadPiece.currentX
        let groupOriginMinY = movedGroup.compactMap { initialPiecePositions[$0.id]?.y }.min() ?? leadPiece.currentY
        let dragDx = Float(movedMinX - groupOriginMinX)
        let dragDy = Float(movedMinY - groupOriginMinY)
        
        // 2. Check if the intended Grid Position is FREE (Priority over Push/Swap)
        let cellW = pieces[0].width
        let cellH = pieces[0].height
        let gridDeltaCol = Int(round(Float(dragDx) / Float(cellW)))
        let gridDeltaRow = Int(round(Float(dragDy) / Float(cellH)))
        
        let originCells = getGridOccupancy(group: movedGroup)
        let intendedCells = offsetGridCells(cells: originCells, dRow: gridDeltaRow, dCol: gridDeltaCol)
        let myGroupId = movedGroup[0].groupId
        let excludeSet = Set([myGroupId])
        
        print("📋 放手目标: 起点格位=\(originCells.map {"(\($0.row),\($0.col))"}.joined()) 放手格位=\(intendedCells.map {"(\($0.row),\($0.col))"}.joined()) delta=(\(gridDeltaRow),\(gridDeltaCol))")
        logMovedGroup(movedGroup, originCells: originCells, intendedCells: intendedCells)
        
        if areGridCellsInBounds(cells: intendedCells) && areGridCellsFree(cells: intendedCells, excludeGroupIds: excludeSet) {
             print("✅ Moving to Free Grid Space 替换成功")
             // 使用已校验的 intendedCells 落子，避免用 getCurrentGridPosition 产生浮点偏差
             moveGroupToGridCells(group: movedGroup, cells: intendedCells)
             let merged = checkNeighborSnapping(movedPiece: leadPiece)
             
             // Tutorial Check
             if isTutorialMode && tutorialStep == 0 && merged {
                 tutorialStep = 1
             }
             
             if merged { 
                 normalizeAllPiecesToGrid()
                 updateHiddenEdges()
                 logInteractionState(prefix: "📋 落子+合并后")
                 checkForWin()
                 return 
             }
             normalizeAllPiecesToGrid()
             logInteractionState(prefix: "📋 落子后")
             checkForWin()
             return
        }
        
        // Find overlapping targets for interaction
        let overlapTargets = findOverlappingTargets(movedGroup: movedGroup)
        if !overlapTargets.isEmpty {
            let overlapIds = overlapTargets.map { $0.id }
            print("📋 重叠目标块 ids=\(overlapIds)")
        }
        
        // 3. Try Interaction (Push / Swap)
        if !overlapTargets.isEmpty {
            // A. Try PUSH first (Chain Push)
            if tryPushInteraction(movedGroup: movedGroup, targets: overlapTargets, dragDx: dragDx, dragDy: dragDy) {
                print("✅ Push Success 挤压成功")
                let merged = checkAllGroupsForSnappingUntilStable()
                
                // Tutorial Check
                if isTutorialMode && tutorialStep == 0 && merged {
                    tutorialStep = 1
                }
                
                normalizeAllPiecesToGrid()
                updateHiddenEdges()
                logInteractionState(prefix: "📋 挤压后")
                checkForWin()
                return
            }
        }
        
        // B. Try SMART SWAP second (Grid-based vacancy filling, works even without precise overlap)
        if trySwapInteraction(movedGroup: movedGroup, dragDx: dragDx, dragDy: dragDy) {
            print("✅ Swap Success 替换成功")
            let merged = checkAllGroupsForSnappingUntilStable()
            
            // Tutorial Check
            if isTutorialMode && tutorialStep == 0 && (merged || isTutorialMode) { // Tutorial special: swap counts
                tutorialStep = 1
            }
            
            normalizeAllPiecesToGrid()
            updateHiddenEdges()
            logInteractionState(prefix: "📋 替换后")
            checkForWin()
            return
        }
        
        // B'. Intent-based swap: 按释放位置所在格子尝试交换（远距离拖拽兜底，如右下→第二行第一格）
        if tryIntentBasedSwap(movedGroup: movedGroup) {
            print("✅ Swap Success (Intent-based) 替换成功")
            let merged = checkAllGroupsForSnappingUntilStable()
            
            // Tutorial Check
            if isTutorialMode && tutorialStep == 0 && (merged || isTutorialMode) {
                tutorialStep = 1
            }
            
            normalizeAllPiecesToGrid()
            updateHiddenEdges()
            logInteractionState(prefix: "📋 意图替换后")
            checkForWin()
            return
        }
        
        // C. No Interaction or Interaction Failed -> Try simple placement
        // If valid placement (even if not perfectly aligned)
        if isValidPlacement(group: movedGroup) {
             print("✅ Valid Placement (Grid Snap) 替换成功")
             _ = tryGridSnap(group: movedGroup)
             if checkNeighborSnapping(movedPiece: leadPiece) { 
                 normalizeAllPiecesToGrid()
                 updateHiddenEdges()
                 logInteractionState(prefix: "📋 有效落子+合并后")
                 checkForWin()
                 return 
             }
             normalizeAllPiecesToGrid()
             updateHiddenEdges()
             logInteractionState(prefix: "📋 有效落子后")
             checkForWin()
             return
        }
        
        // D. Android Step 6: 尝试 Grid Snap 兜底（即使 placement 不完美）
        if tryGridSnap(group: movedGroup) {
             print("✅ Grid Snap (fallback) 替换成功")
             if checkNeighborSnapping(movedPiece: leadPiece) { 
                 normalizeAllPiecesToGrid()
                 updateHiddenEdges()
                 logInteractionState(prefix: "📋 兜底落子+合并后")
                 checkForWin()
                 return 
             }
             normalizeAllPiecesToGrid()
             updateHiddenEdges()
             logInteractionState(prefix: "📋 兜底落子后")
             checkForWin()
             return
        }
        
        // 5. Revert
        print("↩️ Reverting Move 替换失败 回退 groupId=\(groupID) 块ids=\(movedGroup.map {$0.id})")
        revertMove(groupId: groupID)
        PlatformUtils.vibrateError() 
    }


    // MARK: - Algorithms
    
    func checkNeighborSnapping(movedPiece: PuzzlePiece) -> Bool {
        var hasMergedAny = false
        var keepChecking = true
        var totalMergedEdges = 0
        var lastMergeSource: PuzzlePiece?
        var lastSurvivorGroupId: Int? = nil
        
        print("🔍 checkNeighborSnapping for Group \(movedPiece.groupId) 组内块=\(pieces.filter {$0.groupId == movedPiece.groupId}.map {$0.id})")
        
        while keepChecking {
            keepChecking = false
            let currentGroupId = movedPiece.groupId
            let movedGroup = pieces.filter { $0.groupId == currentGroupId }
            let otherPieces = pieces.filter { $0.groupId != currentGroupId }
            
            searchLoop: for current in movedGroup {
                let currentCorrectRow = current.id / self.cols
                let currentCorrectCol = current.id % self.cols
                
                for target in otherPieces {
                    let targetCorrectRow = target.id / self.cols
                    let targetCorrectCol = target.id % self.cols
                    
                    // 1. 严格逻辑相邻校验：只有在原始矩阵中上下左右相邻的块才有资格合并
                    let isRightNeighbor = currentCorrectRow == targetCorrectRow && currentCorrectCol == targetCorrectCol - 1
                    let isLeftNeighbor = currentCorrectRow == targetCorrectRow && currentCorrectCol == targetCorrectCol + 1
                    let isBottomNeighbor = currentCorrectCol == targetCorrectCol && currentCorrectRow == targetCorrectRow - 1
                    let isTopNeighbor = currentCorrectCol == targetCorrectCol && currentCorrectRow == targetCorrectRow + 1
                    
                    guard isRightNeighbor || isLeftNeighbor || isBottomNeighbor || isTopNeighbor else {
                        continue // 逻辑不相邻，绝不允许合并（彻底解决“不相邻黏在一起”的问题）
                    }
                    
                    // 2. 物理位置精准判定
                    var expectedX = target.currentX
                    var expectedY = target.currentY
                    
                    if isRightNeighbor { expectedX -= current.width }
                    if isLeftNeighbor { expectedX += target.width }
                    if isBottomNeighbor { expectedY -= current.height }
                    if isTopNeighbor { expectedY += target.height }
                    
                    let dx = abs(current.currentX - expectedX)
                    let dy = abs(current.currentY - expectedY)
                    
                    // Android parity: 只有在物理距离非常接近且逻辑相邻时才吸附
                    let isTutorial = (rows == 2 && cols == 2) || levelConfig?.levelId == "tutorial_0"
                    let mainFactor: CGFloat = isTutorial ? 0.35 : 0.25
                    let crossFactor: CGFloat = 0.08 // 略微放宽垂直轴容差，提升吸附手感
                    
                    let isHorizontal = isRightNeighbor || isLeftNeighbor
                    let canSnap: Bool
                    if isHorizontal {
                        canSnap = dx < current.width * mainFactor && dy < current.height * crossFactor
                    } else {
                        canSnap = dy < current.height * mainFactor && dx < current.width * crossFactor
                    }
                    
                    if canSnap {
                        // 3. 验证合并后位置是否合法（防冲突）
                        let moveX = expectedX - current.currentX
                        let moveY = expectedY - current.currentY
                        let excludeTarget = Set([target.groupId])
                        if !isPositionValid(group: movedGroup, dx: moveX, dy: moveY, additionalExcludeGroupIds: excludeTarget) {
                            continue
                        }
                        
            
            print("✅ MATCH: 拼图块 \(current.id) 与邻居 \(target.id) 合并")
            let edges = mergeGroups(source: current, target: target, expectedX: expectedX, expectedY: expectedY)
            totalMergedEdges += edges
            lastMergeSource = current
            // Capture survivor group ID (target's group) for scoring
            lastSurvivorGroupId = target.groupId
            hasMergedAny = true
            keepChecking = true
            break searchLoop
                }
            }
        }
    }
        
        if totalMergedEdges > 0, let source = lastMergeSource {
            let centerX = source.currentX + source.width / 2
            let centerY = source.currentY + source.height / 2
            
            // 🚨 关键修复：检查双倍 Buff 状态（对齐 Android 逻辑）
            let isBuffActive = LevelProgressManager.shared.isDoubleCoinsActive()
            let multiplier = isBuffActive ? 2 : 1
            
            // 基础得分：每条边 2 分
            let baseScore = totalMergedEdges * 2
            // 应用 Buff 倍率用于 UI 显示
            let displayScore = baseScore * multiplier
            
            let particleCount = min(24, 6 + totalMergedEdges * 6)
            let colors: [Color] = totalMergedEdges > 1
                ? [Color(hex: 0x00FFFF), Color(hex: 0xFF00FF), Color(hex: 0xFFFF00)]
                : [.red, .yellow, .blue, .green]
            SoundManager.shared.playSnap()
            particleSystem.emit(x: centerX, y: centerY, count: particleCount, colors: colors)
            
            // 飘字显示翻倍后的得分，颜色区分是否双倍
            particleSystem.addFloatingText(
                x: centerX, 
                y: centerY, 
                text: "+\(displayScore)", 
                color: isBuffActive ? .orange : .yellow
            )
            
            // Note: Score update is handled inside mergeGroups to ensure 'paidConnections' check
            // We only handle visual feedback here (particles, text)
            
            // Android parity: Record score for unmerge (使用翻倍后的值)
            if let survivorGroupId = lastSurvivorGroupId {
                self.scoredGroups[survivorGroupId, default: 0] += displayScore
            }

            
            // Tutorial Check: If we merged in tutorial level, advance to next step and clear the hint
            if isTutorialMode && tutorialStep == 0 {
                print("🎓 Tutorial: Auto-advancing to step 1 after merge")
                tutorialStep = 1
                clearHint() // Remove the ghost animation now that they've done it
            }
        }
        
        return hasMergedAny
    }
    
    /// 对所有组进行邻接合并检查，直到稳定（推挤/交换后，被挤过去的块可能与新邻居匹配，需补检）
    func checkAllGroupsForSnappingUntilStable() -> Bool {
        var anyMerged = false
        var keepGoing = true
        while keepGoing {
            keepGoing = false
            let groupIds = Set(pieces.map { $0.groupId })
            for gid in groupIds {
                guard let lead = pieces.first(where: { $0.groupId == gid }) else { continue }
                if checkNeighborSnapping(movedPiece: lead) {
                    anyMerged = true
                    keepGoing = true
                    break
                }
            }
        }
        return anyMerged
    }
    
    func findOverlappingTargets(movedGroup: [PuzzlePiece]) -> [PuzzlePiece] {
        guard let first = movedGroup.first else { return [] }
        
        // Android: 左上角 currentX/Y = 左上角
        let movedMinX = movedGroup.map { $0.currentX }.min() ?? 0
        let movedMinY = movedGroup.map { $0.currentY }.min() ?? 0
        let movedMaxX = movedGroup.map { $0.currentX + $0.width }.max() ?? 0
        let movedMaxY = movedGroup.map { $0.currentY + $0.height }.max() ?? 0
        
        var overlapped: [(PuzzlePiece, CGFloat)] = []
        
        for piece in pieces {
            if piece.groupId == first.groupId { continue }
            
            let pieceLeft = piece.currentX
            let pieceTop = piece.currentY
            let pieceRight = piece.currentX + piece.width
            let pieceBottom = piece.currentY + piece.height
            
            let overlapLeft = max(movedMinX, pieceLeft)
            let overlapTop = max(movedMinY, pieceTop)
            let overlapRight = min(movedMaxX, pieceRight)
            let overlapBottom = min(movedMaxY, pieceBottom)
            
            if overlapRight > overlapLeft && overlapBottom > overlapTop {
                let area = (overlapRight - overlapLeft) * (overlapBottom - overlapTop)
                let pieceArea = piece.width * piece.height
                // Lower threshold for overlap to make interactions feel more responsive
                // Android 20%；略放宽以便 2+1 合并时更容易触发 Push/Swap
                if area / pieceArea > 0.18 {
                    overlapped.append((piece, area))
                }
            }
        }
        
        let targetGroupIds = Set(overlapped.map { $0.0.groupId })
        return pieces.filter { targetGroupIds.contains($0.groupId) }
    }
    
    func tryPushInteraction(movedGroup: [PuzzlePiece], targets: [PuzzlePiece], dragDx: Float, dragDy: Float) -> Bool {
        let isHorizontal = abs(dragDx) > abs(dragDy)
        let deltaRow = !isHorizontal ? (dragDy > 0 ? 1 : -1) : 0
        let deltaCol = isHorizontal ? (dragDx > 0 ? 1 : -1) : 0
        print("📋 tryPushInteraction: 挤压方向 delta=(\(deltaRow),\(deltaCol)) 目标组ids=\(Array(Set(targets.map { $0.groupId })))")
        
        // Build chain
        let targetGroupIds = Set(targets.map { $0.groupId })
        var currentTargets: [[PuzzlePiece]] = []
        for id in targetGroupIds {
            currentTargets.append(pieces.filter { $0.groupId == id })
        }
        
        var pushChain: [[PuzzlePiece]] = []
        var excludeGroups = Set([movedGroup[0].groupId])
        
        var depth = 0
        let maxChainDepth = 10
        
        while !currentTargets.isEmpty && depth < maxChainDepth {
            pushChain.append(contentsOf: currentTargets)
            for group in currentTargets {
                if let id = group.first?.groupId { excludeGroups.insert(id) }
            }
            
            var nextObstacles: [[PuzzlePiece]] = []
            
            for targetGroup in currentTargets {
                let currentOccupancy = getGridOccupancy(group: targetGroup)
                let newOccupancy = offsetGridCells(cells: currentOccupancy, dRow: deltaRow, dCol: deltaCol)
                
                // Check blocks
                for cell in newOccupancy {
                    if let blocker = pieces.first(where: { !excludeGroups.contains($0.groupId) && GridCell(row: $0.row, col: $0.col) == cell }) {
                        let blockingGroup = pieces.filter { $0.groupId == blocker.groupId }
                        // Check if unique group already added
                        let blockerId = blockingGroup[0].groupId
                        if !nextObstacles.contains(where: { $0[0].groupId == blockerId }) {
                            nextObstacles.append(blockingGroup)
                        }
                    }
                }
            }
            currentTargets = nextObstacles
            depth += 1
        }
        
        if depth >= maxChainDepth {
            print("📋 挤压失败: 链深度超限")
            return false
        }
        
        // Validate end of chain
        for i in stride(from: pushChain.count - 1, through: 0, by: -1) {
            let groupToPush = pushChain[i]
            let currentOccupancy = getGridOccupancy(group: groupToPush)
            let newOccupancy = offsetGridCells(cells: currentOccupancy, dRow: deltaRow, dCol: deltaCol)
            
            if !areGridCellsInBounds(cells: newOccupancy) {
                print("📋 挤压失败: 组\(groupToPush[0].groupId) 目标格\(newOccupancy.map {"(\($0.row),\($0.col))"}.joined()) 越界")
                return false
            }
            
            var chainIds = Set(pushChain.flatMap { group in group.map { $0.groupId } })
            chainIds.insert(movedGroup[0].groupId)
            
            if !areGridCellsFree(cells: newOccupancy, excludeGroupIds: chainIds) {
                print("📋 挤压失败: 组\(groupToPush[0].groupId) 目标格被占")
                    return false
            }
        }
        
        // Execute Push
        // Move from end of chain
        print("📋 挤压链 共\(pushChain.count)组 从远到近执行:")
        for i in stride(from: pushChain.count - 1, through: 0, by: -1) {
            let groupToPush = pushChain[i]
            let currentOccupancy = getGridOccupancy(group: groupToPush)
            let newOccupancy = offsetGridCells(cells: currentOccupancy, dRow: deltaRow, dCol: deltaCol)
            let ids = groupToPush.map { $0.id }
            let fromStr = currentOccupancy.map { "(\($0.row),\($0.col))" }.joined(separator: ",")
            let toStr = newOccupancy.map { "(\($0.row),\($0.col))" }.joined(separator: ",")
            print("📋   组\(groupToPush[0].groupId) ids=\(ids) 从\(fromStr) -> \(toStr)")
            moveGroupToGridCells(group: groupToPush, cells: newOccupancy)
        }
        print("📋 挤压完成 各组最终格位: 有组=\(!pushChain.isEmpty)")
        SoundManager.shared.playSound("push")
        resetZIndex()
        return true
    }
    
    /// Android parity: vacatedCells + findValidPlacement 支持多组交换
    func trySwapInteraction(movedGroup: [PuzzlePiece], dragDx: Float, dragDy: Float) -> Bool {
        let cellW = pieces[0].width
        let cellH = pieces[0].height
        
        let deltaCol = Int(round(Float(dragDx) / Float(cellW)))
        let deltaRow = Int(round(Float(dragDy) / Float(cellH)))
        
        if deltaCol == 0 && deltaRow == 0 {
            print("📋 trySwapInteraction: delta=0 跳过")
            return false
        }
        
        let originCells = Set(getGridOccupancy(group: movedGroup))
        let targetCells = Set(offsetGridCells(cells: Array(originCells), dRow: deltaRow, dCol: deltaCol))
        
        if !areGridCellsInBounds(cells: Array(targetCells)) {
            print("📋 trySwapInteraction: 目标格越界 targetCells=\(targetCells.map {"(\($0.row),\($0.col))"}.joined())")
            return false
        }
        
        let movedGroupIds = Set(movedGroup.map { $0.groupId })
        let otherPieces = pieces.filter { !movedGroupIds.contains($0.groupId) }
        // 合并组 row/col 可能不准，用像素反推确保能正确识别目标格上的块
        let conflictingPieces = otherPieces.filter { p in
            let cell = GridCell(row: p.row, col: p.col)
            let cellFromPx = GridCell(row: toGrid(x: p.currentX, y: p.currentY).0, col: toGrid(x: p.currentX, y: p.currentY).1)
            return targetCells.contains(cell) || targetCells.contains(cellFromPx)
        }
        
        if conflictingPieces.isEmpty {
            print("📋 替换: 目标格空闲 直接落位 替换成功")
            moveGroupToGridCells(group: movedGroup, cells: Array(targetCells))
            resetZIndex()
            return true
        }
        
        let conflictingGroupIds = Set(conflictingPieces.map { $0.groupId })
        let conflictingGroups = conflictingGroupIds.map { gid in pieces.filter { $0.groupId == gid } }
        print("📋 trySwapInteraction: 冲突组=\(conflictingGroupIds) 冲突块ids=\(conflictingPieces.map {$0.id}) vacated=\(originCells.subtracting(targetCells).map {"(\($0.row),\($0.col))"}.joined())")
        
        // vacatedCells = 我方离开的格子（origin 有 target 无）
        let vacatedCells = originCells.subtracting(targetCells)
        if vacatedCells.isEmpty {
            // 对称交换：双方块数相同且目标完全重叠时，直接互换位置
            if conflictingGroups.count == 1,
               let targetGroup = conflictingGroups.first,
               targetGroup.count == movedGroup.count {
                let targetOrigin = Set(getGridOccupancy(group: targetGroup))
                let inverseCells = Set(offsetGridCells(cells: Array(targetOrigin), dRow: -deltaRow, dCol: -deltaCol))
                if inverseCells == originCells && areGridCellsInBounds(cells: Array(inverseCells)) {
                    print("📋 替换: 对称交换 替换成功 我方->\(targetCells.map {"(\($0.row),\($0.col))"}.joined()) 对方->\(originCells.map {"(\($0.row),\($0.col))"}.joined())")
                    moveGroupToGridCells(group: targetGroup, cells: Array(originCells))
                    moveGroupToGridCells(group: movedGroup, cells: Array(targetCells))
                    SoundManager.shared.playSound("swap")
                    PlatformUtils.vibrateSuccess()
                    resetZIndex()
                    return true
                }
            }
            return false
        }
        
        var groupMoves: [Int: (Int, Int)] = [:] // groupId -> (dRow, dCol)
        var remainingVacated = vacatedCells
        
        for group in conflictingGroups {
            let groupCells = Set(getGridOccupancy(group: group))
            if let (dR, dC) = findValidPlacement(groupCells: groupCells, availableCells: remainingVacated) {
                groupMoves[group[0].groupId] = (dR, dC)
                let usedCells = Set(offsetGridCells(cells: Array(groupCells), dRow: dR, dCol: dC))
                for c in usedCells { remainingVacated.remove(c) }
            } else if movedGroup.count == 1, vacatedCells.count == 1, targetCells.count == 1,
                      let vacatedCell = vacatedCells.first, let targetCell = targetCells.first,
                      let blockingPiece = group.first(where: {
                          GridCell(row: $0.row, col: $0.col) == targetCell ||
                          GridCell(row: toGrid(x: $0.currentX, y: $0.currentY).0, col: toGrid(x: $0.currentX, y: $0.currentY).1) == targetCell
                      }) {
                // 单格交换：目标格被合并组中的一块占用，拆出该块与我方交换（解决“第二行第三个往第三行第三个移动不了”）
                if let idx = pieces.firstIndex(where: { $0.id == blockingPiece.id }) {
                    let originalGid = group[0].groupId
                    let remainingInGroup = group.filter { $0.id != blockingPiece.id }
                    if !remainingInGroup.isEmpty {
                        let newGid = remainingInGroup.map { $0.id }.min()!
                        for i in pieces.indices where pieces[i].groupId == originalGid && pieces[i].id != blockingPiece.id {
                            pieces[i].groupId = newGid
                        }
                        print("📋 替换: 单格交换(拆组) 原组\(originalGid) 剩余\(remainingInGroup.count)块 -> groupId=\(newGid)")
                    }
                    print("📋 替换: 单格交换(拆组) 替换成功 块\(blockingPiece.id) (\(targetCell.row),\(targetCell.col))->(\(vacatedCell.row),\(vacatedCell.col)) 我方->(\(targetCell.row),\(targetCell.col))")
                    pieces[idx].groupId = pieces[idx].id
                    moveGroupToGridCells(group: [pieces[idx]], cells: [vacatedCell])
                    moveGroupToGridCells(group: movedGroup, cells: [targetCell])
                    SoundManager.shared.playSound("swap")
                    PlatformUtils.vibrateSuccess()
                    resetZIndex()
                    return true
                }
            } else {
                print("📋 替换失败: 组\(group[0].groupId) 无法放入空位 findValidPlacement=nil 块数=\(group.count) 空位数=\(remainingVacated.count)")
                return false
            }
        }
        
        print("📋 替换: 多组交换 替换成功")
        for (groupId, delta) in groupMoves {
            let group = pieces.filter { $0.groupId == groupId }
            let curOccupancy = getGridOccupancy(group: group)
            let newOccupancy = offsetGridCells(cells: curOccupancy, dRow: delta.0, dCol: delta.1)
            print("📋 替换组\(groupId) ids=\(group.map {$0.id}) \((curOccupancy.map {"(\($0.row),\($0.col))"}.joined())) -> \((newOccupancy.map {"(\($0.row),\($0.col))"}.joined()))")
            moveGroupToGridCells(group: group, cells: newOccupancy)
        }
        print("📋 替换移动组 -> \(targetCells.map {"(\($0.row),\($0.col))"}.joined())")
        moveGroupToGridCells(group: movedGroup, cells: Array(targetCells))
        
        SoundManager.shared.playSound("swap")
        PlatformUtils.vibrateSuccess()
        resetZIndex()
        return true
    }
    
    /// 按释放位置意图交换：拖拽松手时，若我方左上角落在其他组的格子上，尝试与对方交换
    /// 解决远距离拖拽（如右下角→第二行第一格）时 delta 舍入或合并组导致 trySwapInteraction 失败
    func tryIntentBasedSwap(movedGroup: [PuzzlePiece]) -> Bool {
        let movedMinX = movedGroup.map { $0.currentX }.min() ?? 0
        let movedMinY = movedGroup.map { $0.currentY }.min() ?? 0
        let (dropRow, dropCol) = toGrid(x: movedMinX, y: movedMinY)
        let dropCell = GridCell(row: dropRow, col: dropCol)
        print("📋 tryIntentBasedSwap: 放手格位=(\(dropRow),\(dropCol))")
        
        let originCells = Set(getGridOccupancy(group: movedGroup))
        if originCells.contains(dropCell) {
            print("📋 tryIntentBasedSwap: 放手格=起点 跳过")
            return false
        }
        
        let myGroupId = movedGroup[0].groupId
        let conflictingPieces = pieces.filter { p in
            guard p.groupId != myGroupId else { return false }
            let (r, c) = toGrid(x: p.currentX, y: p.currentY)
            return r == dropRow && c == dropCol
        }
        if conflictingPieces.isEmpty {
            print("📋 tryIntentBasedSwap: 放手格无冲突块 跳过")
            return false
        }
        
        let targetGroupIds = Set(conflictingPieces.map { $0.groupId })
        let targetGroupsFlat = targetGroupIds.map { gid in pieces.filter { $0.groupId == gid } }
        
        let targetCells = Set(targetGroupsFlat.flatMap { getGridOccupancy(group: $0) })
        let vacatedCells = originCells.subtracting(targetCells)
        if vacatedCells.isEmpty {
            print("📋 tryIntentBasedSwap: vacatedCells空 跳过")
            return false
        }
        print("📋 tryIntentBasedSwap: 冲突块ids=\(conflictingPieces.map {$0.id}) targetCells=\(targetCells.map {"(\($0.row),\($0.col))"}.joined()) vacated=\(vacatedCells.map {"(\($0.row),\($0.col))"}.joined())")
        
        var groupMoves: [(group: [PuzzlePiece], cells: [GridCell])] = []
        var remainingVacated = vacatedCells
        for targetGroup in targetGroupsFlat {
            let groupCells = Set(getGridOccupancy(group: targetGroup))
            if let (dR, dC) = findValidPlacement(groupCells: groupCells, availableCells: remainingVacated) {
                let newOccupancy = offsetGridCells(cells: Array(groupCells), dRow: dR, dCol: dC)
                for c in newOccupancy { remainingVacated.remove(c) }
                groupMoves.append((targetGroup, newOccupancy))
            } else if movedGroup.count == 1, vacatedCells.count == 1,
                      let vacatedCell = vacatedCells.first,
                      let blockingPiece = targetGroup.first(where: {
                          let (r, c) = toGrid(x: $0.currentX, y: $0.currentY)
                          return r == dropRow && c == dropCol
                      }) {
                // 单格交换
                if let idx = pieces.firstIndex(where: { $0.id == blockingPiece.id }) {
                    let originalGid = targetGroup[0].groupId
                    let remainingInGroup = targetGroup.filter { $0.id != blockingPiece.id }
                    if !remainingInGroup.isEmpty {
                        let newGid = remainingInGroup.map { $0.id }.min()!
                        for i in pieces.indices where pieces[i].groupId == originalGid && pieces[i].id != blockingPiece.id {
                            pieces[i].groupId = newGid
                        }
                        print("📋 tryIntentBasedSwap: 单格交换(拆组) 原组\(originalGid) 剩余\(remainingInGroup.count)块 -> groupId=\(newGid)")
                    }
                    print("📋 tryIntentBasedSwap: 单格交换(拆组) 替换成功 块\(blockingPiece.id) ->(\(vacatedCell.row),\(vacatedCell.col)) 我方->(\(dropRow),\(dropCol))")
                    pieces[idx].groupId = pieces[idx].id
                    moveGroupToGridCells(group: [pieces[idx]], cells: [vacatedCell])
                    moveGroupToGridCells(group: movedGroup, cells: [dropCell])
                    SoundManager.shared.playSound("swap")
                    PlatformUtils.vibrateSuccess()
                    resetZIndex()
                    return true
                }
                return false
            } else {
                print("📋 tryIntentBasedSwap: 替换失败 组\(targetGroup[0].groupId) findValidPlacement=nil 块数=\(targetGroup.count)")
                return false
            }
        }
        
        print("📋 tryIntentBasedSwap: 多组交换 替换成功")
        for (targetGroup, newOccupancy) in groupMoves {
            let from = getGridOccupancy(group: targetGroup).map {"(\($0.row),\($0.col))"}.joined()
            let to = newOccupancy.map {"(\($0.row),\($0.col))"}.joined()
            print("📋   组\(targetGroup[0].groupId) ids=\(targetGroup.map {$0.id}) \(from) -> \(to)")
            moveGroupToGridCells(group: targetGroup, cells: newOccupancy)
        }
        print("📋   移动组 -> \(targetCells.map {"(\($0.row),\($0.col))"}.joined())")
        moveGroupToGridCells(group: movedGroup, cells: Array(targetCells))
        SoundManager.shared.playSound("swap")
        PlatformUtils.vibrateSuccess()
        resetZIndex()
        return true
    }

    
    func findValidPlacement(groupCells: Set<GridCell>, availableCells: Set<GridCell>) -> (Int, Int)? {
        if groupCells.isEmpty { return (0, 0) }
        if availableCells.isEmpty { return nil }
        
        let firstCell = groupCells.first!
        
        // Sort available cells by distance
        let sortedAvailable = availableCells.sorted { c1, c2 in
            let dist1 = abs(c1.row - firstCell.row) + abs(c1.col - firstCell.col)
            let dist2 = abs(c2.row - firstCell.row) + abs(c2.col - firstCell.col)
            return dist1 < dist2
        }
        
        for targetCell in sortedAvailable {
            let dRow = targetCell.row - firstCell.row
            let dCol = targetCell.col - firstCell.col
            
            // Check if ALL cells in group shifted fit in available
            let allFit = groupCells.allSatisfy { cell in
                let newCell = GridCell(row: cell.row + dRow, col: cell.col + dCol)
                return availableCells.contains(newCell)
            }
            
            if allFit {
                return (dRow, dCol)
            }
        }
        return nil
    }
    
    func tryGridSnap(group: [PuzzlePiece]) -> Bool {
        let currentCells = getCurrentGridPosition(group: group)
        let excludeGroups = Set([group[0].groupId])
        
        if areGridCellsInBounds(cells: currentCells) && areGridCellsFree(cells: currentCells, excludeGroupIds: excludeGroups) {
            moveGroupToGridCells(group: group, cells: currentCells)
            return true
        }
        
        // Fallback: If current cells are blocked (maybe overlap), try to move pieces back to their SAVED row/col
        // (This happens if user drops a group in a 'partial overlap' but it logically should snap back to its previous cell center)
        let occupancyCells = getCurrentGridPosition(group: group)
        if areGridCellsInBounds(cells: occupancyCells) && areGridCellsFree(cells: occupancyCells, excludeGroupIds: excludeGroups) {
            moveGroupToGridCells(group: group, cells: occupancyCells)
            return true
        }
        
        return false
    }
    
    func revertMove(groupId: Int) {
        // Android parity: Restore to exact drag-start positions AND row/col (PieceState)
        var reverted: [(Int, Int, Int)] = []
        for index in 0..<pieces.count {
            if pieces[index].groupId == groupId {
                let p = pieces[index]
                if let initial = initialPiecePositions[p.id] {
                    pieces[index].currentX = initial.x
                    pieces[index].currentY = initial.y
                }
                if let rc = initialPieceRowCol[p.id] {
                    pieces[index].row = rc.row
                    pieces[index].col = rc.col
                    reverted.append((p.id, rc.row, rc.col))
                }
            }
        }
        print("📋 revertMove: 回退块 ids=\(reverted.map {"\($0.0):(\($0.1),\($0.2))"}.joined(separator: ","))")
        SoundManager.shared.playRevert()
        objectWillChange.send()
    }
    
    // MARK: - Helpers
    
    func mergeGroups(source: PuzzlePiece, target: PuzzlePiece, expectedX: CGFloat, expectedY: CGFloat) -> Int {
        let sourceGroupId = source.groupId
        let targetGroupId = target.groupId
        
        let offsetX = expectedX - source.currentX
        let offsetY = expectedY - source.currentY
        
        // 🥶 Performance Optimization: Copy to local var to avoid repeated @Published updates
        var localPieces = pieces
        
        let sourceGroup = localPieces.filter { $0.groupId == sourceGroupId }
        let targetGroup = localPieces.filter { $0.groupId == targetGroupId }
        
        let sourceGroupIndices = localPieces.indices.filter { localPieces[$0].groupId == sourceGroupId }
        
        for index in sourceGroupIndices {
            localPieces[index].currentX += offsetX
            localPieces[index].currentY += offsetY
            localPieces[index].groupId = targetGroupId
        }
        
        // 合并后同步 row/col（基于目标偏移计算，避免重复，再 fallback 像素坐标）
        let mergedGroupIndices = localPieces.indices.filter { localPieces[$0].groupId == targetGroupId }
        
        let (targetRow, targetCol) = toGrid(x: target.currentX, y: target.currentY)
        let targetSolvedRow = target.id / cols
        let targetSolvedCol = target.id % cols
        let offsetRow = targetRow - targetSolvedRow
        let offsetCol = targetCol - targetSolvedCol
        
        var usedCells = Set<GridCell>()
        
        // Re-assign grid positions for the entire merged group
        for index in mergedGroupIndices {
            let piece = localPieces[index]
            let solvedRow = piece.id / cols
            let solvedCol = piece.id % cols
            let primary = clampToBoard(row: solvedRow + offsetRow, col: solvedCol + offsetCol)
            let assigned = allocateCell(for: piece, primary: primary, usedCells: &usedCells)
            
            localPieces[index].row = assigned.row
            localPieces[index].col = assigned.col
        }
        
        // Commit changes to @Published property ONCE
        pieces = localPieces
        

        
        // Count merged edges + new paid edges (Android parity)
        var mergedEdges = 0
        var newPaidEdges = 0
        var newEdgeKeys: Set<String> = []
        for sPiece in sourceGroup {
            let sCorrectRow = sPiece.id / cols
            let sCorrectCol = sPiece.id % cols
            for tPiece in targetGroup {
                let tCorrectRow = tPiece.id / cols
                let tCorrectCol = tPiece.id % cols
                let isHoriz = sCorrectRow == tCorrectRow && abs(sCorrectCol - tCorrectCol) == 1
                let isVert = sCorrectCol == tCorrectCol && abs(sCorrectRow - tCorrectRow) == 1
                if isHoriz || isVert {
                    mergedEdges += 1
                    let edgeKey = sPiece.id < tPiece.id ? "\(sPiece.id)-\(tPiece.id)" : "\(tPiece.id)-\(sPiece.id)"
                    if !paidConnections.contains(edgeKey) {
                        paidConnections.insert(edgeKey)
                        newPaidEdges += 1
                        newEdgeKeys.insert(edgeKey)
                    }
                }
            }
        }
        
        if mergedEdges > 0 {
            let oldSourceScore = scoredGroups.removeValue(forKey: sourceGroupId) ?? 0
            let oldTargetScore = scoredGroups.removeValue(forKey: targetGroupId) ?? 0
            let mergeScore = newPaidEdges * 2
            let newTotalScore = oldSourceScore + oldTargetScore + mergeScore
            scoredGroups[targetGroupId] = newTotalScore
            // 合并时：目标组 = 目标已有边 + 源组已有边 + 本次新边（拆分时需从 paidConnections 移除该组边以便再次合并加回）
            var targetEdges = paidEdgesByGroup.removeValue(forKey: targetGroupId) ?? []
            let sourceEdges = paidEdgesByGroup.removeValue(forKey: sourceGroupId) ?? []
            targetEdges.formUnion(sourceEdges)
            targetEdges.formUnion(newEdgeKeys)
            paidEdgesByGroup[targetGroupId] = targetEdges
            if newPaidEdges > 0 {
                // 🚨 关键修复：检查 Buff 状态以计算实际显示的得分（对齐 Android sessionCoins）
                let isBuffActive = LevelProgressManager.shared.isDoubleCoinsActive()
                let multiplier = isBuffActive ? 2 : 1
                let actualDisplayScore = mergeScore * multiplier
                
                // addCoins 传入基础分数，内部会根据 Buff 状态翻倍
                LevelProgressManager.shared.addCoins(mergeScore)
                
                // sessionScore 累加翻倍后的值（用于结算界面显示）
                self.sessionScore += actualDisplayScore
                
                print("💰 mergeGroups: baseScore=\(mergeScore) isBuffActive=\(isBuffActive) actualDisplayScore=\(actualDisplayScore) sessionScore=\(self.sessionScore)")
                
                // Trigger top bar bounce
                scoreEventCount += 1
            }
        }
        
        objectWillChange.send()
        return mergedEdges
    }
    
    func isPositionValid(group: [PuzzlePiece], dx: CGFloat, dy: CGFloat, additionalExcludeGroupIds: Set<Int> = []) -> Bool {
        var excludeGroups = additionalExcludeGroupIds
        excludeGroups.insert(group[0].groupId)
        
        let newCells = group.map { p in
            let (r, c) = toGrid(x: p.currentX + dx, y: p.currentY + dy)
            return GridCell(row: r, col: c)
        }
        
        if !areGridCellsInBounds(cells: newCells) {
            print("❌ isPositionValid: Out of Bounds \(newCells)")
            return false
        }
        
        for cell in newCells {
            if let blocker = pieces.first(where: { p in
                guard !excludeGroups.contains(p.groupId) else { return false }
                let (r, c) = toGrid(x: p.currentX, y: p.currentY)
                return r == cell.row && c == cell.col
            }) {
                print("❌ isPositionValid: Blocked by Piece \(blocker.id) at (\(cell.row), \(cell.col))")
                return false
            }
        }
        
        return true
    }
    
    private func clampToBoard(row: Int, col: Int) -> GridCell {
        GridCell(
            row: max(0, min(rows - 1, row)),
            col: max(0, min(cols - 1, col))
        )
    }
    
    private func allocateCell(for piece: PuzzlePiece, primary: GridCell, usedCells: inout Set<GridCell>) -> GridCell {
        var candidates: [GridCell] = [primary]
        let pixelCellTuple = toGrid(x: piece.currentX, y: piece.currentY)
        let pixelCell = GridCell(row: pixelCellTuple.row, col: pixelCellTuple.col)
        if !candidates.contains(pixelCell) {
            candidates.append(pixelCell)
        }
        let solvedCell = GridCell(row: piece.id / cols, col: piece.id % cols)
        if !candidates.contains(solvedCell) {
            candidates.append(solvedCell)
        }
        
        for cell in candidates where !usedCells.contains(cell) {
            usedCells.insert(cell)
            return cell
        }
        
        for r in 0..<rows {
            for c in 0..<cols {
                let cell = GridCell(row: r, col: c)
                if !usedCells.contains(cell) {
                    usedCells.insert(cell)
                    return cell
                }
            }
        }
        
        usedCells.insert(primary)
        return primary
    }
    
    func isValidPlacement(group: [PuzzlePiece]) -> Bool {
        let currentCells = getCurrentGridPosition(group: group)
        let excludeGroups = Set([group[0].groupId])
        return areGridCellsInBounds(cells: currentCells) && areGridCellsFree(cells: currentCells, excludeGroupIds: excludeGroups)
    }
    
    /// Android: 从 currentX/Y（左上角）反推网格
    func getCurrentGridPosition(group: [PuzzlePiece]) -> [GridCell] {
        group.map { p in
            let (r, c) = toGrid(x: p.currentX, y: p.currentY)
            return GridCell(row: r, col: c)
        }
    }
    
    func getGridOccupancy(group: [PuzzlePiece]) -> [GridCell] {
        return group.map { GridCell(row: $0.row, col: $0.col) }
    }
    
    func offsetGridCells(cells: [GridCell], dRow: Int, dCol: Int) -> [GridCell] {
        return cells.map { GridCell(row: $0.row + dRow, col: $0.col + dCol) }
    }
    
    func areGridCellsInBounds(cells: [GridCell]) -> Bool {
        for cell in cells {
            if cell.row < 0 || cell.row >= self.rows || cell.col < 0 || cell.col >= self.cols {
                return false
            }
        }
        return true
    }
    
    /// Android: 用 piece.row, piece.col 判断占用
    func areGridCellsFree(cells: [GridCell], excludeGroupIds: Set<Int>) -> Bool {
        for cell in cells {
            if pieces.contains(where: { !excludeGroupIds.contains($0.groupId) && $0.row == cell.row && $0.col == cell.col }) {
                return false
            }
        }
        return true
    }
    
    /// Android parity: 按相对位置建立 源格->目标格 映射
    func moveGroupToGridCells(group: [PuzzlePiece], cells: [GridCell]) {
        guard !group.isEmpty, !cells.isEmpty else { return }
        
        // 单块或块数与格子数一致时，直接按顺序映射
        if group.count == 1 && cells.count == 1 {
            let targetCell = cells[0]
            if let index = pieces.firstIndex(where: { $0.id == group[0].id }) {
                let tl = gridTopLeft(row: targetCell.row, col: targetCell.col)
                pieces[index].currentX = tl.x
                pieces[index].currentY = tl.y
                pieces[index].targetX = tl.x
                pieces[index].targetY = tl.y
                pieces[index].row = targetCell.row
                pieces[index].col = targetCell.col
            }
            objectWillChange.send()
            return
        }
        
        // Android: 用 piece.row/col 建立映射
        let sourceCells = getGridOccupancy(group: group)
        let sourceMinRow = sourceCells.map { $0.row }.min() ?? 0
        let sourceMinCol = sourceCells.map { $0.col }.min() ?? 0
        let targetMinRow = cells.map { $0.row }.min() ?? 0
        let targetMinCol = cells.map { $0.col }.min() ?? 0
        
        for piece in group {
            let offsetRow = piece.row - sourceMinRow
            let offsetCol = piece.col - sourceMinCol
            let targetCell = GridCell(row: targetMinRow + offsetRow, col: targetMinCol + offsetCol)
            
            if let index = pieces.firstIndex(where: { $0.id == piece.id }) {
                let tl = gridTopLeft(row: targetCell.row, col: targetCell.col)
                pieces[index].currentX = tl.x
                pieces[index].currentY = tl.y
                pieces[index].targetX = tl.x
                pieces[index].targetY = tl.y
                pieces[index].row = targetCell.row
                pieces[index].col = targetCell.col
            }
        }
        objectWillChange.send()
    }
    
    func logGridState(reason: String) {
        var grid: [String: [Int]] = [:]
        for p in pieces {
            let key = "\(p.row),\(p.col)"
            grid[key, default: []].append(p.id)
        }
        let empty: [String] = (0..<rows).flatMap { r in (0..<cols).map { c in "\(r),\(c)" } }.filter { (grid[$0]?.count ?? 0) == 0 }
        let overlap: [(String, [Int])] = grid.filter { $0.value.count > 1 }.map { ($0.key, $0.value) }
        if !empty.isEmpty { print("📋 \(reason): 空格子 \(empty)") }
        if !overlap.isEmpty { print("📋 \(reason): 重叠 \(overlap)") }
    }
    
    func resetZIndex() {
        // pieces.sort { $0.zIndex < $1.zIndex } // SwiftUI zIndex is declarative, but array order helps?
        // Actually we use .zIndex modifier in View.
        // We should normalize zIndices
        let sorted = pieces.sorted { $0.zIndex < $1.zIndex }
        for (i, p) in sorted.enumerated() {
            if let idx = pieces.firstIndex(where: {$0.id == p.id}) {
                pieces[idx].zIndex = i
            }
        }
    }
}
