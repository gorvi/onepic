package site.aiok.onepic.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import site.aiok.onepic.model.PuzzlePiece
import site.aiok.onepic.audio.SoundManager
import site.aiok.onepic.audio.SoundType
import kotlin.math.abs

class GameBoardView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    companion object {
        private const val TAG = "GameBoardView"
    }

    private var pieces = mutableListOf<PuzzlePiece>()
    
    // Grid dimensions (calculated from pieces)
    private var gridRows = 0
    private var gridCols = 0
    private var selectedPiece: PuzzlePiece? = null
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // Store initial positions of the currently dragged group for snap-back
    private data class PieceState(val x: Float, val y: Float, val row: Int, val col: Int)
    private var dragStartStates = mutableMapOf<Int, PieceState>()

    // Puzzle boundaries for clamping drag
    private var puzzleBounds = android.graphics.RectF()
    
    // 暴露图片的实际渲染高度，用于调整白色边框
    var actualImageHeight: Float = 0f
        private set

    // Particle System
    private val particleSystem = ParticleSystem()
    
    // Callback for puzzle completion
    var onPuzzleComplete: ((timeInSeconds: Int) -> Unit)? = null
    
    // Callback for score changes (merge: +score, unmerge: -score)
    var onScoreChange: ((scoreDelta: Int) -> Unit)? = null
    
    // Track which groups have been scored (to prevent duplicate scoring)
    // Key: groupId, Value: score earned for this merge
    private val scoredGroups = mutableMapOf<Int, Int>()
    
    // Timer
    private var startTime: Long = 0
    private var elapsedSeconds: Int = 0
    private var isTimerRunning = false
    
    // Track if level is already completed to prevent duplicate win triggers
    private var isLevelCompleted = false

    // Handler for delayed callbacks
    private val handler = Handler(Looper.getMainLooper())
    
    // Sound Manager
    private val soundManager by lazy { SoundManager.getInstance(context) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        particleSystem.setDimensions(w, h)
        updateLayout(w, h)
    }

    private fun updateLayout(viewWidth: Int, viewHeight: Int) {
        if (pieces.isEmpty()) return

        // 1. Infer grid dimensions if not already set (assuming rectangular grid)
        val rows = pieces.maxOf { it.id } / pieces.maxOf { (it.id % (pieces.maxOf { p -> p.col } + 1)) + 1 } + 1
        // Re-infer rows/cols safely using ID, assuming ID 0 is top-left and increments by row-major
        // Max ID is (rows * cols) - 1.
        // We need to be careful. Let's assume pieces cover the grid fully.
        // Max(row) and Max(col) might be shuffled, but the MAX value is constant.
        // But wait! In setPieces we shuffle row/col. So max(row) and max(col) are still valid indicators of grid size.
        val maxRow = pieces.maxOf { it.row }
        val maxCol = pieces.maxOf { it.col }
        val gridRows = maxRow + 1
        val gridCols = maxCol + 1

        // 2. Calculate Layout - 减少padding，让拼图填充更多空间
        // 调试：分析为什么白框没有完全包围图片
        // 问题1: GameBoardView内部有padding = 16f，导致图片内容不填满整个View
        // 问题2: AndroidView有padding(horizontal = 4.dp, vertical = 1.dp)，进一步缩小了内容区域
        // 问题3: Box有padding(horizontal = 8.dp)，也会影响布局
        // 解决方案：将GameBoardView的padding设为0，让图片完全填满View
        val padding = 0f  // 改为0，让图片完全填满View
        val availableWidth = viewWidth - padding * 2
        val availableHeight = viewHeight - padding * 2
        
        // Use the first piece's bitmap to determine original aspect ratio
        val firstPiece = pieces[0]
        val originalPieceWidth = firstPiece.bitmap.width
        val originalPieceHeight = firstPiece.bitmap.height
        val totalOriginalWidth = originalPieceWidth * gridCols
        val totalOriginalHeight = originalPieceHeight * gridRows
        
        val scale = minOf(
            availableWidth / totalOriginalWidth,
            availableHeight / totalOriginalHeight
        )
        
        val finalWidth = totalOriginalWidth * scale
        val finalHeight = totalOriginalHeight * scale
        
        // 保存图片的实际渲染高度，用于调整白色边框
        actualImageHeight = finalHeight
        
        // 修改：让图片从顶部开始对齐，而不是居中，这样白色边框可以紧贴上边缘
        val offsetX = (viewWidth - finalWidth) / 2  // 水平居中
        val offsetY = 0f  // 从顶部开始，紧贴上边缘
        
        // Define puzzle bounds
        puzzleBounds.set(offsetX, offsetY, offsetX + finalWidth, offsetY + finalHeight)
        
        val newPieceWidth = finalWidth / gridCols
        val newPieceHeight = finalHeight / gridRows
        
        // Update all pieces based on their CURRENT grid position
        pieces.forEach { piece ->
            piece.width = newPieceWidth
            piece.height = newPieceHeight
            
            // Current Pixel Position (Strict Grid Alignment - NO GAPS)
            piece.currentX = offsetX + piece.col * newPieceWidth
            piece.currentY = offsetY + piece.row * newPieceHeight
            
            // Target Pixel Position (Derived from ID)
            // ID = correctRow * gridCols + correctCol
            val correctRow = piece.id / gridCols
            val correctCol = piece.id % gridCols
            
            piece.targetX = offsetX + correctCol * newPieceWidth
            piece.targetY = offsetY + correctRow * newPieceHeight
        }
    }

    fun setPieces(newPieces: List<PuzzlePiece>) {
        pieces.clear()
        scoredGroups.clear()  // Reset score tracking for new game
        isLevelCompleted = false  // Reset completion flag for new game
        
        // Infer grid dimensions first to enable shuffling
        if (newPieces.isNotEmpty()) {
            // Assumption: The input pieces are fresh from slicer, so rows/cols can be inferred from max(row/col)
            // which correspond to the CORRECT positions.
            gridRows = newPieces.maxOf { it.row } + 1
            gridCols = newPieces.maxOf { it.col } + 1
            
            // Create shuffled grid slots
            val slots = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until gridRows) {
                for (c in 0 until gridCols) {
                    slots.add(Pair(r, c))
                }
            }
            slots.shuffle()
            
            // Assign shuffled slots to pieces (updating their CURRENT position)
            newPieces.forEachIndexed { index, piece ->
                if (index < slots.size) {
                    piece.row = slots[index].first
                    piece.col = slots[index].second
                    // piece.groupId is already unique from slicer
                }
            }
        }
        
        pieces.addAll(newPieces)
        pieces.sortBy { it.zIndex }
        
        // Start timer
        startTime = System.currentTimeMillis()
        elapsedSeconds = 0
        isTimerRunning = true
        
        // Trigger layout update if we already have dimensions
        if (width > 0 && height > 0) {
            updateLayout(width, height)
        }
        invalidate()
    }
    
    fun getElapsedSeconds(): Int {
        if (isTimerRunning) {
            elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        }
        return elapsedSeconds
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw all pieces in z-index order (lower zIndex drawn first, higher zIndex on top)
        pieces.sortedBy { it.zIndex }.forEach { piece ->
            // Draw the bitmap scaled to piece dimensions
            val dstRect = android.graphics.RectF(
                piece.currentX,
                piece.currentY,
                piece.currentX + piece.width,
                piece.currentY + piece.height
            )
            canvas.drawBitmap(piece.bitmap, null, dstRect, null)
        }
        
        // Update and draw particles
        particleSystem.update()
        particleSystem.draw(canvas)
        
        // Keep animating if there are active particles or in fireworks mode
        if (particleSystem.isActive()) {
            invalidate()
        }
    }


    private val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            
            // Find piece under tap
            val candidate = pieces.sortedByDescending { it.zIndex }
                .firstOrNull { 
                    x >= it.currentX && x <= it.currentX + it.width && 
                    y >= it.currentY && y <= it.currentY + it.height 
                }
                
            if (candidate != null) {
                val groupPieces = pieces.filter { it.groupId == candidate.groupId }
                if (groupPieces.size > 1) {
                    val groupId = candidate.groupId
                    
                    // Check if this group was scored, and deduct points if so
                    // When unmerging, we deduct the total score for this group
                    // This prevents cheating by repeatedly merging/unmerging
                    val scoreToDeduct = scoredGroups.remove(groupId)
                    if (scoreToDeduct != null && scoreToDeduct > 0) {
                        onScoreChange?.invoke(-scoreToDeduct)
                    }
                    
                    // Unmerge: Assign a new unique ID to EACH piece
                    // Revert each piece to its own ID as groupId
                    // Also clear any score tracking for individual pieces (they're now separate)
                    groupPieces.forEach { piece ->
                        piece.groupId = piece.id
                        // Also bring them to front slightly to indicate change?
                        piece.zIndex += 100
                    }
                    // Re-sort z-indices
                    pieces.sortBy { it.zIndex }
                    pieces.forEachIndexed { index, p -> p.zIndex = index }
                    
                    invalidate()
                    return true
                }
            }
            return false
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Find the top-most piece under the finger
                val candidate = pieces.sortedByDescending { it.zIndex }
                    .firstOrNull { 
                        x >= it.currentX && x <= it.currentX + it.width && 
                        y >= it.currentY && y <= it.currentY + it.height 
                    }

                if (candidate != null) {
                    selectedPiece = candidate
                    
                    bringGroupToFront(candidate.groupId)
                    
                    // Record start positions for snap-back
                    dragStartStates.clear()
                    pieces.filter { it.groupId == candidate.groupId }.forEach { 
                        dragStartStates[it.id] = PieceState(it.currentX, it.currentY, it.row, it.col)
                    }
                    
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                selectedPiece?.let { piece ->
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    
                    // Move ALL pieces in the same group
                    val groupPieces = pieces.filter { it.groupId == piece.groupId }
                    
                    // Calculate the new bounding box of the group
                    var minX = Float.MAX_VALUE
                    var minY = Float.MAX_VALUE
                    var maxX = Float.MIN_VALUE
                    var maxY = Float.MIN_VALUE
                    
                    groupPieces.forEach { 
                        minX = minOf(minX, it.currentX + dx)
                        minY = minOf(minY, it.currentY + dy)
                        maxX = maxOf(maxX, it.currentX + it.width + dx)
                        maxY = maxOf(maxY, it.currentY + it.height + dy)
                    }
                    
                    // --- BOUNDARY CONSTRAINT ---
                    // Limit movement so the group stays strictly inside the PUZZLE AREA (original canvas)
                    var finalDx = dx
                    var finalDy = dy
                    
                    // Calculate absolute bounds if we applied full dx/dy
                    val newMinX = minX
                    val newMinY = minY
                    val newMaxX = maxX
                    val newMaxY = maxY
                    
                    // Clamp deltas to keep inside puzzleBounds
                    if (newMinX < puzzleBounds.left) finalDx = puzzleBounds.left - groupPieces.minOf { it.currentX }
                    if (newMinY < puzzleBounds.top) finalDy = puzzleBounds.top - groupPieces.minOf { it.currentY }
                    
                    if (newMaxX > puzzleBounds.right) finalDx = puzzleBounds.right - groupPieces.maxOf { it.currentX + it.width }
                    if (newMaxY > puzzleBounds.bottom) finalDy = puzzleBounds.bottom - groupPieces.maxOf { it.currentY + it.height }
                    
                        groupPieces.forEach { groupPiece ->
                        groupPiece.currentX += finalDx
                        groupPiece.currentY += finalDy
                        }
                    lastTouchX += finalDx // Update based on actual movement
                    lastTouchY += finalDy
                    
                        invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedPiece?.let { piece ->
                    
                    handleActionUp(piece)
                    
                    // Check Win Condition
                    checkForWin()
                    
                    selectedPiece = null
                    invalidate()
                }
            }
        }
        return true
    }

    /**
     * Centralized handler for piece release interactions.
     * Strategy:
     * 1. Try to SNAP to neighbors (highest priority for puzzle building)
     * 2. Try to INTERACT with overlapped pieces (Push or Swap)
     * 3. If no interaction, Try to SNAP to Grid (clean placement)
     * 4. If invalid placement, REVERT to start position
     */
    private fun handleActionUp(piece: PuzzlePiece) {
        // 1. Check Snapping (Merge)
        if (checkNeighborSnapping(piece)) {
            return
        }

        // 2. Check Interaction (Swap / Push)
        // We attempt to resolve any collisions by Pushing or Swapping
                        val movedGroup = pieces.filter { it.groupId == piece.groupId }
        val movedMinX = movedGroup.minOf { it.currentX }
        val movedMinY = movedGroup.minOf { it.currentY }
        
        // Calculate total drag vector from start
        val startState = dragStartStates[piece.id]
        val startX = startState?.x ?: piece.currentX
        val startY = startState?.y ?: piece.currentY
        
        // Use the group's original position to calculate drag vector
        val groupOriginMinX = movedGroup.minOf { dragStartStates[it.id]?.x ?: it.currentX }
        val groupOriginMinY = movedGroup.minOf { dragStartStates[it.id]?.y ?: it.currentY }
        
        val dragDx = movedMinX - groupOriginMinX
        val dragDy = movedMinY - groupOriginMinY

        // 2. Check if the intended Grid Position is FREE (Priority over Push/Swap)
        // Even if there is pixel overlap during drag, if the snapped grid position is empty,
        // we should just place it there (Fit/Embed) instead of triggering interactions.
        val cellW = pieces[0].width
        val cellH = pieces[0].height
        val gridDeltaCol = kotlin.math.round(dragDx / cellW).toInt()
        val gridDeltaRow = kotlin.math.round(dragDy / cellH).toInt()
        
        val originCells = getGridOccupancy(movedGroup)
        val intendedCells = offsetGridCells(originCells, gridDeltaRow, gridDeltaCol)
        val myGroupId = movedGroup[0].groupId
        
        if (areGridCellsInBounds(intendedCells) && areGridCellsFree(intendedCells, setOf(myGroupId))) {
             tryGridSnap(piece, movedGroup)
             checkNeighborSnapping(piece)
             return
        }

        // Find overlapping targets
        val overlapTargets = findOverlappingTargets(movedGroup)
        
        // 3. Try Interaction (Push / Swap)
        if (overlapTargets.isNotEmpty()) {
            // A. Try PUSH first (Chain Push)
            if (tryPushInteraction(movedGroup, overlapTargets, dragDx, dragDy)) {
                checkNeighborSnapping(piece)
                return
            }
        }
            
        // B. Try SMART SWAP second (Grid-based vacancy filling, works even without precise overlap)
        if (trySwapInteraction(movedGroup, dragDx, dragDy)) {
            checkNeighborSnapping(piece)
            return
        }
        
        // C. No Interaction or Interaction Failed -> Try simple placement
        // No overlap, checking for valid placement
        if (isValidPlacement(movedGroup)) {
             // Try to align to grid for neatness even if valid
             tryGridSnap(piece, movedGroup)
             checkNeighborSnapping(piece) // Check snapping after placement
             return
        }
        
        // If slightly overlapping or misaligned (but not significantly enough to trigger Swap/Push),
        // try to snap to nearest grid
        if (tryGridSnap(piece, movedGroup)) {
            checkNeighborSnapping(piece)
            return
        }

        // 4. Revert
        revertMove(piece)
    }

    // --- Helper Methods for Logic Consolidation ---

    private fun findOverlappingTargets(movedGroup: List<PuzzlePiece>): List<PuzzlePiece> {
        val movedMinX = movedGroup.minOf { it.currentX }
        val movedMinY = movedGroup.minOf { it.currentY }
        val movedMaxX = movedGroup.maxOf { it.currentX + it.width }
        val movedMaxY = movedGroup.maxOf { it.currentY + it.height }

        val overlapped = mutableListOf<Pair<PuzzlePiece, Float>>()
        
        for (piece in pieces) {
            if (piece.groupId == movedGroup[0].groupId) continue
            
            val overlapLeft = maxOf(movedMinX, piece.currentX)
            val overlapTop = maxOf(movedMinY, piece.currentY)
            val overlapRight = minOf(movedMaxX, piece.currentX + piece.width)
            val overlapBottom = minOf(movedMaxY, piece.currentY + piece.height)
            
            if (overlapRight > overlapLeft && overlapBottom > overlapTop) {
                val area = (overlapRight - overlapLeft) * (overlapBottom - overlapTop)
                val pieceArea = piece.width * piece.height
                // Filter significant overlaps (>20%)
                if (area / pieceArea > 0.2f) {
                    overlapped.add(Pair(piece, area))
                }
            }
        }
        
        // Get all unique groups from overlapped pieces
        val targetGroupIds = overlapped.map { it.first.groupId }.distinct()
        return pieces.filter { it.groupId in targetGroupIds }
    }

    private fun tryPushInteraction(
        movedGroup: List<PuzzlePiece>, 
        targets: List<PuzzlePiece>, 
        dragDx: Float, 
        dragDy: Float
    ): Boolean {
        // 基于网格的推挤逻辑
        // 1. 确定推挤方向（网格级别）
        val isHorizontal = abs(dragDx) > abs(dragDy)
        val deltaRow = if (!isHorizontal) (if (dragDy > 0) 1 else -1) else 0
        val deltaCol = if (isHorizontal) (if (dragDx > 0) 1 else -1) else 0
        
        // 2. 获取所有参与交互的组
        val targetGroupIds = targets.map { it.groupId }.distinct()
        val allTargetGroups = mutableListOf<List<PuzzlePiece>>()
        
        targetGroupIds.forEach { groupId ->
            allTargetGroups.add(pieces.filter { it.groupId == groupId })
        }
        
        // 3. 尝试链式推挤
        val pushChain = mutableListOf<List<PuzzlePiece>>()
        val excludeGroups = mutableSetOf(movedGroup[0].groupId)
        
        // 从第一个目标开始构建推挤链
        var currentTargets = allTargetGroups
        var depth = 0
        val MAX_CHAIN_DEPTH = 10
        
        while (currentTargets.isNotEmpty() && depth < MAX_CHAIN_DEPTH) {
            // 将当前这批目标加入推挤链
            pushChain.addAll(currentTargets)
            excludeGroups.addAll(currentTargets.flatMap { it.map { p -> p.groupId } })
            
            // 计算这批目标推挤后的新位置
            val nextObstacles = mutableListOf<List<PuzzlePiece>>()
            
            for (targetGroup in currentTargets) {
                val currentOccupancy = getGridOccupancy(targetGroup)
                val newOccupancy = offsetGridCells(currentOccupancy, deltaRow, deltaCol)
                
                // 检查新位置是否被其他组占据
                val blockedCells = newOccupancy.filter { cell ->
                    pieces.any { piece -> 
                        piece.groupId !in excludeGroups && 
                        GridCell(piece.row, piece.col) == cell 
                                }
                            }
                            
                if (blockedCells.isNotEmpty()) {
                    // 找到阻挡的组
                    val blockingGroupIds = blockedCells.mapNotNull { cell ->
                        pieces.firstOrNull { GridCell(it.row, it.col) == cell && it.groupId !in excludeGroups }?.groupId
                    }.distinct()
                    
                    blockingGroupIds.forEach { groupId ->
                        val blockingGroup = pieces.filter { it.groupId == groupId }
                        if (blockingGroup !in nextObstacles) {
                            nextObstacles.add(blockingGroup)
                        }
                    }
                }
            }
            
            currentTargets = nextObstacles
            depth++
        }
        
        if (depth >= MAX_CHAIN_DEPTH) {
            return false
        }
                            
        // 4. 验证整个推挤链的最终位置
        // 从链条末尾开始检查（最远的那个必须能移动到空格子）
        for (i in pushChain.size - 1 downTo 0) {
            val groupToPush = pushChain[i]
            val currentOccupancy = getGridOccupancy(groupToPush)
            val newOccupancy = offsetGridCells(currentOccupancy, deltaRow, deltaCol)
            
            // 检查边界
            if (!areGridCellsInBounds(newOccupancy)) {
                return false
            }
            
            // 检查空间（排除推挤链中的所有组）
            val excludeInCheck = pushChain.flatMap { it.map { p -> p.groupId } }.toSet() + movedGroup[0].groupId
            if (!areGridCellsFree(newOccupancy, excludeInCheck)) {
                return false
            }
        }
        
        // 5. 执行推挤（从链条末尾开始移动，避免冲突）
        for (i in pushChain.size - 1 downTo 0) {
            val groupToPush = pushChain[i]
            val currentOccupancy = getGridOccupancy(groupToPush)
            val newOccupancy = offsetGridCells(currentOccupancy, deltaRow, deltaCol)
            
            moveGroupToGridCells(groupToPush, newOccupancy)
        }
        
        // 播放推挤音效
        soundManager.playSound(SoundType.PUSH, 0.6f)
        
        resetZIndex()
        return true
    }

    private fun trySwapInteraction(
        movedGroup: List<PuzzlePiece>,
        dragDx: Float,
        dragDy: Float
    ): Boolean {
        val cellW = pieces[0].width
        val cellH = pieces[0].height
        
        // 1. Calculate intended move delta in Grid coordinates
        val deltaCol = kotlin.math.round(dragDx / cellW).toInt()
        val deltaRow = kotlin.math.round(dragDy / cellH).toInt()
        
        if (deltaCol == 0 && deltaRow == 0) {
             return false
        }

        // 2. Identify Origin and Target Cells
        val originCells = getGridOccupancy(movedGroup)
        val targetCells = offsetGridCells(originCells, deltaRow, deltaCol)
        
        // 3. Boundary Check
        if (!areGridCellsInBounds(targetCells)) {
            return false
        }
        
        // 4. Identify Conflicting Pieces (The ones we want to swap with)
        val excludeIds = movedGroup.map { it.id }.toSet()
        // Only check pieces that are NOT in the moved group
        // And are in the target cells
        val conflictingPieces = pieces.filter { 
            it.id !in excludeIds && targetCells.contains(GridCell(it.row, it.col))
        }
        
        if (conflictingPieces.isEmpty()) {
             moveGroupToGridCells(movedGroup, targetCells)
             resetZIndex()
             return true
        }

        // 5. Expand Conflicts to Full Groups (Preserve Groups Rule)
        val conflictingGroupIds = conflictingPieces.map { it.groupId }.distinct()
        val fullConflictingGroups = pieces.filter { it.groupId in conflictingGroupIds }
        
        // Group pieces by ID for processing
        val conflictingGroupsList = fullConflictingGroups.groupBy { it.groupId }.values.toList()

        // 6. Identify Vacated Cells (The holes we can fill)
        // Vacated = Origin - Target (Cells that were occupied, but will be free)
        // Paranoid check: Explicitly ensure no target cells are in vacated list
        val vacatedCells = originCells.filter { !targetCells.contains(it) }.toMutableSet()
        
        if (vacatedCells.isEmpty()) {
             return false
        }
        
        // 7. Try to fit each conflicting group into Vacated Cells
        val groupMoves = mutableMapOf<Int, Pair<Int, Int>>() // groupId -> (dRow, dCol)
        
        for (group in conflictingGroupsList) {
            val groupCells = getGridOccupancy(group)
            val groupId = group[0].groupId
            
            // Try to find a placement
            val validDelta = findValidPlacement(groupCells, vacatedCells)
            
            if (validDelta != null) {
                groupMoves[groupId] = validDelta
                // Remove used cells from pool
                val usedCells = offsetGridCells(groupCells, validDelta.first, validDelta.second)
                vacatedCells.removeAll(usedCells)
                        } else {
                return false
            }
        }
        
        // 8. Execute Swap
        // Move Conflicting Groups
        groupMoves.forEach { (groupId, delta) ->
            val group = pieces.filter { it.groupId == groupId }
            val currentPos = getGridOccupancy(group)
            val newPos = offsetGridCells(currentPos, delta.first, delta.second)
            
            // Verify we are not moving into TargetCells (Safety check)
            if (newPos.any { targetCells.contains(it) }) {
                 Log.e(TAG, "CRITICAL ERROR: Conflicting group $groupId moving to TargetCells! Aborting swap.")
                 // This should not happen if vacatedCells is correct, but if it does, we must abort or we get overlap.
                 // We can't easily abort here since we might have moved previous groups.
                 // Ideally we check ALL groups before moving ANY.
                 // But for now, let's just Log.
            }
            
            moveGroupToGridCells(group, newPos)
                    }
                    
        // Move Moved Group
        moveGroupToGridCells(movedGroup, targetCells)
        
        // 播放交换音效
        soundManager.playSound(SoundType.SWAP, 0.7f)
        
        resetZIndex()
        return true
    }

    private fun findValidPlacement(groupCells: Set<GridCell>, availableCells: Set<GridCell>): Pair<Int, Int>? {
        // Optimization: We only need to check shifts that map the first cell of the group to ONE of the available cells.
        if (groupCells.isEmpty()) return Pair(0, 0)
        if (availableCells.isEmpty()) return null
        
        val firstCell = groupCells.first()
        
        // Sort available cells by distance to firstCell to prefer closer placements (minimizing movement)
        val sortedAvailable = availableCells.sortedBy { 
            kotlin.math.abs(it.row - firstCell.row) + kotlin.math.abs(it.col - firstCell.col) 
        }
        
        // Try mapping the first cell of the group to every available cell
        for (targetCell in sortedAvailable) {
            val dRow = targetCell.row - firstCell.row
            val dCol = targetCell.col - firstCell.col
            
            // Check if ALL cells in group shifted by (dRow, dCol) are in availableCells
            val allFit = groupCells.all { cell ->
                val newCell = GridCell(cell.row + dRow, cell.col + dCol)
                availableCells.contains(newCell)
            }
            
            if (allFit) {
                return Pair(dRow, dCol)
            }
        }
        
        return null
    }
    
    // LEGACY: 基于 RectF 的碰撞检测（已被网格逻辑替代，保留用于调试）
    // private fun isCollisionFree(...) { ... }
    
    private fun resetZIndex() {
        pieces.forEachIndexed { index, piece -> piece.zIndex = index }
        pieces.sortBy { it.zIndex }
    }

    private fun revertMove(piece: PuzzlePiece) {
        dragStartStates.forEach { (id, state) ->
            val p = pieces.find { it.id == id }
            if (p != null) {
                p.currentX = state.x
                p.currentY = state.y
                p.row = state.row
                p.col = state.col
            }
        }
        // 播放回退音效
        soundManager.playSound(SoundType.REVERT, 0.5f)
    }

    private fun tryGridSnap(piece: PuzzlePiece, group: List<PuzzlePiece>): Boolean {
        // 基于网格的对齐逻辑
        // 1. 计算组的当前网格位置
        val currentCells = getCurrentGridPosition(group)
        
        // 2. 检查这些格子是否为空（排除自己）
        val excludeGroups = setOf(group[0].groupId)
        
        if (areGridCellsInBounds(currentCells) && areGridCellsFree(currentCells, excludeGroups)) {
            // 3. 移动到最近的网格对齐位置
            moveGroupToGridCells(group, currentCells)
            return true
        }

        return false
    }

    // LEGACY: 旧的基于 RectF 的交互逻辑（已被网格逻辑替代）
    // 已删除旧代码块

    private fun isValidPlacement(group: List<PuzzlePiece>): Boolean {
        // 基于网格的有效性检查
        val currentCells = getCurrentGridPosition(group)
        val excludeGroups = setOf(group[0].groupId)
        
        val isInBounds = areGridCellsInBounds(currentCells)
        val isFree = areGridCellsFree(currentCells, excludeGroups)
        
        return isInBounds && isFree
    }

    private fun checkForWin() {
        if (pieces.isEmpty()) return
        
        // 如果关卡已经完成，不再重复触发胜利逻辑
        if (isLevelCompleted) return
        
        val firstGroupId = pieces[0].groupId
        val allSameGroup = pieces.all { it.groupId == firstGroupId }
        
        if (allSameGroup && !particleSystem.isFireworksMode) {
            // 标记关卡已完成，防止重复触发
            isLevelCompleted = true
            
            particleSystem.isFireworksMode = true
            isTimerRunning = false
            elapsedSeconds = getElapsedSeconds() // Capture final time
            
            // 播放完成音效
            soundManager.playSound(SoundType.COMPLETE, 1.0f)
            
            invalidate()
            // 延迟1.5秒后触发完成回调，让用户先看到完成的图片和烟花效果
            handler.postDelayed({
                post {
                    onPuzzleComplete?.invoke(elapsedSeconds)
                }
            }, 1500) // 1.5秒延迟
        }
    }

    private fun bringGroupToFront(groupId: Int) {
        val maxZ = pieces.maxOfOrNull { it.zIndex } ?: 0
        val groupPieces = pieces.filter { it.groupId == groupId }
        
        groupPieces.forEachIndexed { index, piece ->
            piece.zIndex = maxZ + 1 + index
        }
        
        pieces.sortBy { it.zIndex }
    }

    private fun checkNeighborSnapping(movedPiece: PuzzlePiece): Boolean {
        var hasMergedAny = false
        var keepChecking = true
        var totalMergedEdges = 0
        var lastMergeSource: PuzzlePiece? = null
        
        // Loop until no more merges occur in a pass
        while (keepChecking) {
            keepChecking = false
            
            // Refresh the group pieces as they might have changed after a merge
            val currentGroupId = movedPiece.groupId
            val movedGroup = pieces.filter { it.groupId == currentGroupId }
            val otherPieces = pieces.filter { it.groupId != currentGroupId }
            
            // We need to break out of the inner loops if a merge happens
            // to restart the check with the updated group
            searchLoop@ for (current in movedGroup) {
                // Use CORRECT logical positions (derived from ID) to check for True Neighbors
                // We should only snap pieces that actually belong together in the final puzzle.
                val currentCorrectRow = current.id / gridCols
                val currentCorrectCol = current.id % gridCols

                // Stricter threshold to prevent accidental snapping of distant pieces
                // Using 15% of width/height as the snap distance
                val snapThresholdX = current.width * 0.15f
                val snapThresholdY = current.height * 0.15f
                
                for (target in otherPieces) {
                    val targetCorrectRow = target.id / gridCols
                    val targetCorrectCol = target.id % gridCols
                    
                    // Check if they are neighbors in the SOLUTION (not just current grid)
                    val isRightNeighbor = currentCorrectRow == targetCorrectRow && currentCorrectCol == targetCorrectCol - 1
                    val isLeftNeighbor = currentCorrectRow == targetCorrectRow && currentCorrectCol == targetCorrectCol + 1
                    val isBottomNeighbor = currentCorrectCol == targetCorrectCol && currentCorrectRow == targetCorrectRow - 1
                    val isTopNeighbor = currentCorrectCol == targetCorrectCol && currentCorrectRow == targetCorrectRow + 1

                    if (isRightNeighbor || isLeftNeighbor || isBottomNeighbor || isTopNeighbor) {
                        var expectedX = target.currentX
                        var expectedY = target.currentY

                        if (isRightNeighbor) expectedX -= current.width
                        if (isLeftNeighbor) expectedX += target.width
                        if (isBottomNeighbor) expectedY -= current.height
                        if (isTopNeighbor) expectedY += target.height

                        val dx = kotlin.math.abs(current.currentX - expectedX)
                        val dy = kotlin.math.abs(current.currentY - expectedY)
                        
                        // Stricter snapping logic:
                        // Must be close to the target position AND aligned in the other axis
                        val isHorizontal = isRightNeighbor || isLeftNeighbor
                        
                        // STRICTER THRESHOLDS:
                        // Main axis distance: allow 15% tolerance (easy to snap)
                        // Cross axis alignment: allow ONLY 5% tolerance (must be aligned)
                        // This prevents "corner snapping" where pieces attach diagonally.
                        
                        val canSnap = if (isHorizontal) {
                            // For horizontal neighbors, vertical alignment (dy) must be very strict
                            dx < current.width * 0.15f && dy < current.height * 0.05f
                        } else { // isVertical
                            // For vertical neighbors, horizontal alignment (dx) must be very strict
                            dy < current.height * 0.15f && dx < current.width * 0.05f
                        }

                        if (canSnap) {
                            // Calculate the move delta
                            val moveX = expectedX - current.currentX
                            val moveY = expectedY - current.currentY
                            
                            // Verify that moving here doesn't cause overlap with ANY pieces (including target group)
                            if (isPositionValid(movedGroup, moveX, moveY)) {
                                val edges = mergeGroups(current, target, expectedX, expectedY)
                                totalMergedEdges += edges
                                lastMergeSource = current

                                hasMergedAny = true
                                keepChecking = true // Continue checking for more merges
                                break@searchLoop // Restart the search with the new, larger group
                            }
                        }
                    }
                }
            }
        }
        
        // Trigger SINGLE celebration for the entire interaction
        if (totalMergedEdges > 0 && lastMergeSource != null) {
            val source = lastMergeSource!!
            val centerX = source.currentX + source.width / 2
            val centerY = source.currentY + source.height / 2
            
            // Scale fireworks: Reduced count for performance (Base 15, +10 per extra edge)
            val particleCount = 15 + (totalMergedEdges * 10)
            
            // Use different colors for higher combos
            val colors = if (totalMergedEdges > 1) {
                listOf(android.graphics.Color.CYAN, android.graphics.Color.MAGENTA, android.graphics.Color.YELLOW)
            } else {
                listOf(android.graphics.Color.RED, android.graphics.Color.YELLOW, android.graphics.Color.BLUE, android.graphics.Color.GREEN)
            }
            
            particleSystem.emit(centerX, centerY, particleCount, colors)
            
            // Show floating text
            val textColor = android.graphics.Color.rgb(255, 215, 0) // Gold
            particleSystem.addFloatingText(centerX, centerY, "+$totalMergedEdges", textColor)
            
            // Bring merged group to front
            bringGroupToFront(movedPiece.groupId)
        }
        
        if (hasMergedAny) {
            // 播放合并音效
            soundManager.playSound(SoundType.SNAP, 0.8f)
            checkForWin()
            invalidate()
        }
        
        return hasMergedAny
    }

    private fun isPositionValid(group: List<PuzzlePiece>, dx: Float, dy: Float): Boolean {
        // 模拟移动并检查网格有效性
        val cellW = pieces[0].width
        val cellH = pieces[0].height
        val excludeGroups = setOf(group[0].groupId)
        
        // 计算移动后的网格位置
        val newCells = group.map { piece ->
            val newX = piece.currentX + dx
            val newY = piece.currentY + dy
            val relativeX = newX - puzzleBounds.left
            val relativeY = newY - puzzleBounds.top
            val col = kotlin.math.round(relativeX / cellW).toInt()
            val row = kotlin.math.round(relativeY / cellH).toInt()
            GridCell(row.coerceIn(0, gridRows - 1), col.coerceIn(0, gridCols - 1))
        }.toSet()
        
        return areGridCellsInBounds(newCells) && areGridCellsFree(newCells, excludeGroups)
    }

    // LEGACY: 旧的基于 RectF 的链式推挤逻辑（已被网格逻辑替代）
    // 已删除旧代码块

    // ===== GRID OCCUPANCY HELPERS =====
    
    /**
     * 数据类：表示一个网格位置
     */
    private data class GridCell(val row: Int, val col: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is GridCell) return false
            return row == other.row && col == other.col
        }

        override fun hashCode(): Int {
            return 31 * row + col
        }
    }
    
    /**
     * 获取指定组的网格占用集合
     * @param group 要检查的拼图片段组
     * @return 该组占据的所有格子坐标集合
     */
    private fun getGridOccupancy(group: List<PuzzlePiece>): Set<GridCell> {
        return group.map { GridCell(it.row, it.col) }.toSet()
    }
    
    /**
     * 计算当前某个组在像素坐标系下对应的网格位置
     * @param group 要检查的拼图片段组
     * @return 该组当前位置占据的格子坐标集合
     */
    private fun getCurrentGridPosition(group: List<PuzzlePiece>): Set<GridCell> {
        if (group.isEmpty() || pieces.isEmpty()) return emptySet()
        
        val cellW = pieces[0].width
        val cellH = pieces[0].height
        
        return group.map { piece ->
            val relativeX = piece.currentX - puzzleBounds.left
            val relativeY = piece.currentY - puzzleBounds.top
            
            val col = kotlin.math.round(relativeX / cellW).toInt()
            val row = kotlin.math.round(relativeY / cellH).toInt()
            
            GridCell(row.coerceIn(0, gridRows - 1), col.coerceIn(0, gridCols - 1))
        }.toSet()
    }
    
    /**
     * 检查指定的格子集合是否全部为空（未被其他组占据）
     * @param cells 要检查的格子集合
     * @param excludeGroups 排除的组ID列表（例如正在移动的组）
     * @return 如果所有格子都为空则返回 true
     */
    private fun areGridCellsFree(cells: Set<GridCell>, excludeGroups: Set<Int>): Boolean {
        // 构建被占用格子的映射
        val occupiedCells = mutableSetOf<GridCell>()
        
        pieces.filter { it.groupId !in excludeGroups }.forEach { piece ->
            occupiedCells.add(GridCell(piece.row, piece.col))
        }
        
        // 检查目标格子是否与已占用格子冲突
        return cells.all { it !in occupiedCells }
    }
    
    /**
     * 验证指定格子集合是否在网格边界内
     */
    private fun areGridCellsInBounds(cells: Set<GridCell>): Boolean {
        return cells.all { it.row in 0 until gridRows && it.col in 0 until gridCols }
    }
    
    /**
     * 计算格子偏移后的新位置
     * @param cells 原始格子集合
     * @param deltaRow 行偏移量
     * @param deltaCol 列偏移量
     * @return 偏移后的格子集合
     */
    private fun offsetGridCells(cells: Set<GridCell>, deltaRow: Int, deltaCol: Int): Set<GridCell> {
        return cells.map { GridCell(it.row + deltaRow, it.col + deltaCol) }.toSet()
    }
    
    /**
     * 将组移动到指定的网格位置
     * @param group 要移动的组
     * @param targetCells 目标网格位置集合
     * @return 是否成功移动
     */
    private fun moveGroupToGridCells(group: List<PuzzlePiece>, targetCells: Set<GridCell>): Boolean {
        if (group.size != targetCells.size) {
                    return false
                }
        
        val cellW = pieces[0].width
        val cellH = pieces[0].height
        
        // 建立源格子到目标格子的映射
        val sourceToTarget = mutableMapOf<GridCell, GridCell>()
        
        // 按照相对位置建立映射
        val sourceOccupancy = getGridOccupancy(group)
        val sourceMinRow = sourceOccupancy.minOf { it.row }
        val sourceMinCol = sourceOccupancy.minOf { it.col }
        val targetMinRow = targetCells.minOf { it.row }
        val targetMinCol = targetCells.minOf { it.col }
        
        group.forEach { piece ->
            val sourceCell = GridCell(piece.row, piece.col)
            val offsetRow = piece.row - sourceMinRow
            val offsetCol = piece.col - sourceMinCol
            val targetCell = GridCell(targetMinRow + offsetRow, targetMinCol + offsetCol)
            
            sourceToTarget[sourceCell] = targetCell
        }
        
        // 应用移动（同时更新像素位置和逻辑网格位置）
        group.forEach { piece ->
            val sourceCell = GridCell(piece.row, piece.col)
            val targetCell = sourceToTarget[sourceCell]
            
            if (targetCell != null) {
                // 更新像素位置
                piece.currentX = puzzleBounds.left + targetCell.col * cellW
                piece.currentY = puzzleBounds.top + targetCell.row * cellH
                
                // ⚠️ 关键修复: 更新逻辑网格位置
                piece.row = targetCell.row
                piece.col = targetCell.col
            }
        }
        
        return true
    }

    private fun mergeGroups(source: PuzzlePiece, target: PuzzlePiece, alignX: Float, alignY: Float): Int {
        val offsetX = alignX - source.currentX
        val offsetY = alignY - source.currentY

        val sourceGroupId = source.groupId
        val targetGroupId = target.groupId
        
        val sourceGroup = pieces.filter { it.groupId == sourceGroupId }
        val targetGroup = pieces.filter { it.groupId == targetGroupId }

        // Update positions and Group IDs
        sourceGroup.forEach { 
            it.currentX += offsetX
            it.currentY += offsetY
            it.groupId = targetGroupId 
        }
        
        // Calculate merged edges count
        var mergedEdges = 0
        
        // Check connections between the *moved* source group and the *stationary* target group
        // Note: After merging, they are technically in the same group, but we want to count
        // the NEW connections formed between the two original sets of pieces.
        for (sPiece in sourceGroup) {
            val sCorrectRow = sPiece.id / gridCols
            val sCorrectCol = sPiece.id % gridCols
            
            for (tPiece in targetGroup) {
                val tCorrectRow = tPiece.id / gridCols
                val tCorrectCol = tPiece.id % gridCols
                
                // Check if they are neighbors in the SOLUTION (Logic similar to checkNeighborSnapping)
                val isHorizontalNeighbor = sCorrectRow == tCorrectRow && kotlin.math.abs(sCorrectCol - tCorrectCol) == 1
                val isVerticalNeighbor = sCorrectCol == tCorrectCol && kotlin.math.abs(sCorrectRow - tCorrectRow) == 1
                
                if (isHorizontalNeighbor || isVerticalNeighbor) {
                    mergedEdges++
                }
            }
        }
        
        // Calculate and award score for this merge
        // Score: 1 point per merged edge (only count NEW edges created by this merge)
        // The mergedEdges count represents the NEW connections between source and target groups
        if (mergedEdges > 0) {
            // Always score new edges created by this merge operation
            // Remove old group scores (they're now part of the merged group)
            val oldSourceScore = scoredGroups.remove(sourceGroupId) ?: 0
            val oldTargetScore = scoredGroups.remove(targetGroupId) ?: 0
            
            // The new group's score = old scores + new edges from this merge
            val newTotalScore = oldSourceScore + oldTargetScore + mergedEdges
            scoredGroups[targetGroupId] = newTotalScore
            
            // Only award points for the NEW edges (mergedEdges)
            onScoreChange?.invoke(mergedEdges)
        }
        
        return mergedEdges
    }

}

