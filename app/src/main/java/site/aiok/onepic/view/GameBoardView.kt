package site.aiok.onepic.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import site.aiok.onepic.model.PuzzlePiece
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

    // Paint for borders
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }



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

        // 2. Calculate Layout
        val padding = 32f
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
        
        val offsetX = (viewWidth - finalWidth) / 2
        val offsetY = (viewHeight - finalHeight) / 2
        
        // Define puzzle bounds
        puzzleBounds.set(offsetX, offsetY, offsetX + finalWidth, offsetY + finalHeight)
        
        val newPieceWidth = finalWidth / gridCols
        val newPieceHeight = finalHeight / gridRows
        
        Log.d(TAG, "updateLayout: puzzleBounds=$puzzleBounds, cellSize=($newPieceWidth x $newPieceHeight)")
        
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
            
            // Draw dynamic borders
            drawDynamicBorders(canvas, piece)
        }
        
        // Update and draw particles
        particleSystem.update()
        particleSystem.draw(canvas)
        
        // Keep animating if there are active particles or in fireworks mode
        if (particleSystem.isActive()) {
            invalidate()
        }
    }

    private fun drawDynamicBorders(canvas: Canvas, piece: PuzzlePiece) {
        // Find neighbors in the SAME group
        val groupMembers = pieces.filter { it.groupId == piece.groupId }
        
        val hasTop = groupMembers.any { it.row == piece.row - 1 && it.col == piece.col }
        val hasBottom = groupMembers.any { it.row == piece.row + 1 && it.col == piece.col }
        val hasLeft = groupMembers.any { it.row == piece.row && it.col == piece.col - 1 }
        val hasRight = groupMembers.any { it.row == piece.row && it.col == piece.col + 1 }

        val x = piece.currentX
        val y = piece.currentY
        val w = piece.width.toFloat()
        val h = piece.height.toFloat()

        // Draw lines where there is NO neighbor
        if (!hasTop) canvas.drawLine(x, y, x + w, y, borderPaint)
        if (!hasBottom) canvas.drawLine(x, y + h, x + w, y + h, borderPaint)
        if (!hasLeft) canvas.drawLine(x, y, x, y + h, borderPaint)
        if (!hasRight) canvas.drawLine(x + w, y, x + w, y + h, borderPaint)
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
                    Log.d(TAG, "Double tap detected: Unmerging group $groupId with ${groupPieces.size} pieces")
                    
                    // Check if this group was scored, and deduct points if so
                    // When unmerging, we deduct the total score for this group
                    // This prevents cheating by repeatedly merging/unmerging
                    val scoreToDeduct = scoredGroups.remove(groupId)
                    if (scoreToDeduct != null && scoreToDeduct > 0) {
                        onScoreChange?.invoke(-scoreToDeduct)
                        Log.d(TAG, "onDoubleTap: Deducted $scoreToDeduct points for unmerging group $groupId")
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
                    Log.d(TAG, "ACTION_DOWN: Selected piece[${candidate.id}] at (${candidate.currentX}, ${candidate.currentY}) groupId=${candidate.groupId}")
                    
                    bringGroupToFront(candidate.groupId)
                    
                    // Record start positions for snap-back
                    dragStartStates.clear()
                    pieces.filter { it.groupId == candidate.groupId }.forEach { 
                        dragStartStates[it.id] = PieceState(it.currentX, it.currentY, it.row, it.col)
                        Log.d(TAG, "  Drag start: piece[${it.id}] at (${it.currentX}, ${it.currentY}) Grid(${it.row}, ${it.col})")
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
                    Log.d(TAG, "ACTION_UP: piece[${piece.id}] at (${piece.currentX}, ${piece.currentY})")
                    
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
            Log.d(TAG, "handleActionUp: Merged with neighbor.")
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
             Log.d(TAG, "handleActionUp: Intended grid slot is free. Snapping.")
             tryGridSnap(piece, movedGroup)
             checkNeighborSnapping(piece)
             return
        }

        // Find overlapping targets
        val overlapTargets = findOverlappingTargets(movedGroup)
        
        // 3. Try Interaction (Push / Swap)
        if (overlapTargets.isNotEmpty()) {
            Log.d(TAG, "handleActionUp: Overlap detected with ${overlapTargets.size} pieces. Attempting interaction.")
            
            // A. Try PUSH first (Chain Push)
            if (tryPushInteraction(movedGroup, overlapTargets, dragDx, dragDy)) {
                Log.d(TAG, "handleActionUp: Push successful.")
                checkNeighborSnapping(piece)
                return
            }
        }
            
        // B. Try SMART SWAP second (Grid-based vacancy filling, works even without precise overlap)
        if (trySwapInteraction(movedGroup, dragDx, dragDy)) {
            Log.d(TAG, "handleActionUp: Swap successful.")
            checkNeighborSnapping(piece)
            return
        }
        
        // C. No Interaction or Interaction Failed -> Try simple placement
        // No overlap, checking for valid placement
        if (isValidPlacement(movedGroup)) {
             Log.d(TAG, "handleActionUp: Valid placement in empty space.")
             // Try to align to grid for neatness even if valid
             tryGridSnap(piece, movedGroup)
             checkNeighborSnapping(piece) // Check snapping after placement
             return
        }
        
        // If slightly overlapping or misaligned (but not significantly enough to trigger Swap/Push),
        // try to snap to nearest grid
        if (tryGridSnap(piece, movedGroup)) {
            Log.d(TAG, "handleActionUp: Snapped to nearest grid slot.")
            checkNeighborSnapping(piece)
            return
        }

        // 4. Revert
        Log.d(TAG, "handleActionUp: Action failed. Reverting.")
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
        Log.d(TAG, "tryPushInteraction: Attempting GRID-BASED push")
        
        // 1. 确定推挤方向（网格级别）
        val isHorizontal = abs(dragDx) > abs(dragDy)
        val deltaRow = if (!isHorizontal) (if (dragDy > 0) 1 else -1) else 0
        val deltaCol = if (isHorizontal) (if (dragDx > 0) 1 else -1) else 0
        
        Log.d(TAG, "  Push direction: deltaRow=$deltaRow, deltaCol=$deltaCol")
        
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
            Log.d(TAG, "tryPushInteraction: Chain too deep, aborting")
            return false
        }
        
        Log.d(TAG, "  Push chain depth: ${pushChain.size} groups")
                            
        // 4. 验证整个推挤链的最终位置
        // 从链条末尾开始检查（最远的那个必须能移动到空格子）
        for (i in pushChain.size - 1 downTo 0) {
            val groupToPush = pushChain[i]
            val currentOccupancy = getGridOccupancy(groupToPush)
            val newOccupancy = offsetGridCells(currentOccupancy, deltaRow, deltaCol)
            
            // 检查边界
            if (!areGridCellsInBounds(newOccupancy)) {
                Log.d(TAG, "  Push blocked: Group out of bounds")
                return false
            }
            
            // 检查空间（排除推挤链中的所有组）
            val excludeInCheck = pushChain.flatMap { it.map { p -> p.groupId } }.toSet() + movedGroup[0].groupId
            if (!areGridCellsFree(newOccupancy, excludeInCheck)) {
                Log.d(TAG, "  Push blocked: Target cells occupied")
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
        
        Log.d(TAG, "tryPushInteraction: SUCCESS - Pushed ${pushChain.size} groups")
        resetZIndex()
        return true
    }

    private fun trySwapInteraction(
        movedGroup: List<PuzzlePiece>,
        dragDx: Float,
        dragDy: Float
    ): Boolean {
        Log.d(TAG, "trySwapInteraction: Attempting SMART GRID swap")
        
        val cellW = pieces[0].width
        val cellH = pieces[0].height
        
        // 1. Calculate intended move delta in Grid coordinates
        val deltaCol = kotlin.math.round(dragDx / cellW).toInt()
        val deltaRow = kotlin.math.round(dragDy / cellH).toInt()
        
        if (deltaCol == 0 && deltaRow == 0) {
             Log.d(TAG, "  Swap blocked: No grid movement detected")
             return false
        }

        // 2. Identify Origin and Target Cells
        val originCells = getGridOccupancy(movedGroup)
        val targetCells = offsetGridCells(originCells, deltaRow, deltaCol)
        
        // 3. Boundary Check
        if (!areGridCellsInBounds(targetCells)) {
            Log.d(TAG, "  Swap blocked: Target out of bounds")
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
             Log.d(TAG, "  Swap converted to Move: Target is empty")
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
        
        Log.d(TAG, "  Origin: ${originCells.size}, Target: ${targetCells.size}, Vacated: ${vacatedCells.size}")
        if (vacatedCells.isEmpty()) {
             Log.d(TAG, "  Swap blocked: No vacated cells available (Full overlap?)")
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
                Log.d(TAG, "  Swap blocked: Group $groupId does not fit into vacated space")
                return false
            }
        }
        
        // 8. Execute Swap
        Log.d(TAG, "  Swapping: MovedGroup -> Target, ConflictingGroups -> Vacated Slots")
        
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
                            
                            val groupPieces = pieces.filter { it.groupId == piece.groupId }
                            groupPieces.forEach { 
                                Log.d(TAG, "      piece[${it.id}] snapped back to (${it.currentX}, ${it.currentY}) Grid(${it.row}, ${it.col})")
            }
        }

    private fun tryGridSnap(piece: PuzzlePiece, group: List<PuzzlePiece>): Boolean {
        // 基于网格的对齐逻辑
        Log.d(TAG, "tryGridSnap: Attempting GRID-BASED snap")
        
        // 1. 计算组的当前网格位置
        val currentCells = getCurrentGridPosition(group)
        
        Log.d(TAG, "  Current grid cells: $currentCells")
        
        // 2. 检查这些格子是否为空（排除自己）
        val excludeGroups = setOf(group[0].groupId)
        
        if (areGridCellsInBounds(currentCells) && areGridCellsFree(currentCells, excludeGroups)) {
            // 3. 移动到最近的网格对齐位置
            moveGroupToGridCells(group, currentCells)
            Log.d(TAG, "  Grid snap SUCCESS")
        return true
    }

        Log.d(TAG, "  Grid snap FAILED (cells occupied or out of bounds)")
        return false
    }

    // LEGACY: 旧的基于 RectF 的交互逻辑（已被网格逻辑替代）
    /* 
    private fun tryInteraction(touchedPiece: PuzzlePiece): Boolean {
        val movedGroup = pieces.filter { it.groupId == touchedPiece.groupId }
        
        // Calculate bounding box of moved group
        val movedMinX = movedGroup.minOf { it.currentX }
        val movedMinY = movedGroup.minOf { it.currentY }
        val movedMaxX = movedGroup.maxOf { it.currentX + it.width }
        val movedMaxY = movedGroup.maxOf { it.currentY + it.height }
        
        // Find ALL pieces that overlap with the moved group
        val overlappedPieces = mutableListOf<Pair<PuzzlePiece, Float>>()
        
        for (piece in pieces) {
            if (piece.groupId == touchedPiece.groupId) continue
            
            // Check if there's any overlap
            val overlapLeft = maxOf(movedMinX, piece.currentX)
            val overlapTop = maxOf(movedMinY, piece.currentY)
            val overlapRight = minOf(movedMaxX, piece.currentX + piece.width)
            val overlapBottom = minOf(movedMaxY, piece.currentY + piece.height)
            
            if (overlapRight > overlapLeft && overlapBottom > overlapTop) {
                val area = (overlapRight - overlapLeft) * (overlapBottom - overlapTop)
                overlappedPieces.add(Pair(piece, area))
            }
        }
        
        if (overlappedPieces.isEmpty()) {
            return false
        }
        
        // Identify significant targets.
        //  - 如果移动的大块覆盖了目标的小块，就必须把这些小块全部纳入“目标组”，否则会导致碰撞。
        //  - 对于“仅覆盖部分”的情况，将阈值降低到 5%，并且只要有一定面积（例如 50px²）也要算。
        val significantTargets = overlappedPieces.filter { (piece, area) ->
            val pieceArea = piece.width * piece.height
            val overlapRatio = area / pieceArea
            overlapRatio > 0.05f || area > 50f
        }.map { it.first }
        
        val targetPieces: List<PuzzlePiece>
        
        if (significantTargets.isNotEmpty()) {
            val targetGroupIds = significantTargets.map { it.groupId }.distinct()
            targetPieces = pieces.filter { it.groupId in targetGroupIds }
            Log.d(TAG, "tryInteraction: Targeted ${targetGroupIds.size} Groups (${targetPieces.size} pieces) for Interaction")
        } else {
            // Fallback: Pick the single piece with maximum overlap area
            val maxOverlap = overlappedPieces.maxByOrNull { it.second }
            if (maxOverlap != null && maxOverlap.second / (maxOverlap.first.width * maxOverlap.first.height) > 0.2f) {
                val targetGroupId = maxOverlap.first.groupId
                targetPieces = pieces.filter { it.groupId == targetGroupId }
                Log.d(TAG, "tryInteraction: No significant targets, fallback to Group $targetGroupId (>20% overlap)")
            } else {
                return false
            }
        }
        
        if (targetPieces.isEmpty()) return false
        
        
        
        Log.d(TAG, "tryInteraction: SWAP DETECTED")
        Log.d(TAG, "  Moved group has ${movedGroup.size} pieces, bounds: ($movedMinX, $movedMinY) to ($movedMaxX, $movedMaxY)")
        Log.d(TAG, "  Swapping with ${targetPieces.size} pieces from ${targetPieces.map { it.groupId }.distinct().size} different groups")
        
        // Calculate bounding box of target pieces
        val targetMinX = targetPieces.minOf { it.currentX }
        val targetMinY = targetPieces.minOf { it.currentY }
        val targetMaxX = targetPieces.maxOf { it.currentX + it.width }
        val targetMaxY = targetPieces.maxOf { it.currentY + it.height }
        
        val targetWidth = targetMaxX - targetMinX
        val targetHeight = targetMaxY - targetMinY
        
        // Calculate bounding box of moved group at ORIGINAL position
        val movedGroupOriginalMinX = movedGroup.minOf { dragStartPositions[it.id]?.first ?: it.currentX }
        val movedGroupOriginalMinY = movedGroup.minOf { dragStartPositions[it.id]?.second ?: it.currentY }
        val movedGroupOriginalMaxX = movedGroup.maxOf { (dragStartPositions[it.id]?.first ?: it.currentX) + it.width }
        val movedGroupOriginalMaxY = movedGroup.maxOf { (dragStartPositions[it.id]?.second ?: it.currentY) + it.height }
        
        val movedWidth = movedGroupOriginalMaxX - movedGroupOriginalMinX
        val movedHeight = movedGroupOriginalMaxY - movedGroupOriginalMinY
        
        // Calculate deltas for traditional position exchange
        // Moved group moves to Target's current position (top-left aligned)
        val deltaX_MtoT = targetMinX - movedMinX
        val deltaY_MtoT = targetMinY - movedMinY
        
        // Target group moves to Moved group's ORIGINAL position (top-left aligned)
        // CRITICAL CHANGE: Instead of simply aligning top-left to top-left,
        // we need to intelligently fit the target group into the bounding box of the moved group's original hole.
        
        // 1. If the target group fits within the hole dimensions, centered alignment or top-left alignment works.
        // 2. BUT, if the shapes are irregular, a simple bounding box swap might cause collision with neighbors
        //    even if they are just swapping "slots".
        
        // To robustly solve "Squeezing", we should check if the target group can fit into the moved group's original footprint.
        // Since we are in a grid, and assuming we are swapping adjacent logical "blocks", 
        // we try to align the target group's bounding box to the moved group's original bounding box.
        
        // Determine drag direction for intelligent alignment
        val dragDx = movedMinX - movedGroupOriginalMinX
        val dragDy = movedMinY - movedGroupOriginalMinY
        val isHorizontalDrag = abs(dragDx) > abs(dragDy)

        var deltaX_Target = 0f
        var deltaY_Target = 0f

        val movedOriginalCenterX = movedGroupOriginalMinX + movedWidth / 2f
        val movedOriginalCenterY = movedGroupOriginalMinY + movedHeight / 2f
        val targetCenterX = targetMinX + targetWidth / 2f
        val targetCenterY = targetMinY + targetHeight / 2f

        if (isHorizontalDrag) {
            if (dragDx > 0) { // Moving Right -> Target goes to Left of Origin
                // Align Target's Left to Origin's Left
                deltaX_Target = movedGroupOriginalMinX - targetMinX
                // Y aligns Center
                deltaY_Target = movedOriginalCenterY - targetCenterY 
            } else { // Moving Left -> Target goes to Right of Origin
                // Align Target's Right to Origin's Right
                deltaX_Target = (movedGroupOriginalMaxX - targetWidth) - targetMinX
                deltaY_Target = movedOriginalCenterY - targetCenterY
            }
        } else {
            if (dragDy > 0) { // Moving Down -> Target goes to Top of Origin
                // Align Target's Top to Origin's Top
                deltaX_Target = movedOriginalCenterX - targetCenterX
                deltaY_Target = movedGroupOriginalMinY - targetMinY
            } else { // Moving Up -> Target goes to Bottom of Origin
                // Align Target's Bottom to Origin's Bottom
                deltaX_Target = movedOriginalCenterX - targetCenterX
                deltaY_Target = (movedGroupOriginalMaxY - targetHeight) - targetMinY
            }
        }
        
        // Refinement: If groups are equal size, perfect Top-Left alignment is always safest.
        // This handles floating point noise better than center alignment.
        val sizeTolerance = 10f
        if (abs(movedWidth - targetWidth) < sizeTolerance && 
            abs(movedHeight - targetHeight) < sizeTolerance && 
            movedGroup.size == targetPieces.size) {
             deltaX_Target = movedGroupOriginalMinX - targetMinX
             deltaY_Target = movedGroupOriginalMinY - targetMinY
             Log.d(TAG, "checkSwap: Equal size & count groups detected. Using Top-Left alignment.")
        }

        // --- PUSH LOGIC (Try to push target to empty space first) ---
        // Determine direction of drag based on where the moved group is relative to its origin
        // Only consider push if we are overlapping.
        
        // (dragDx, dragDy, isHorizontalDrag already calculated above for alignment logic)
        
        // Calculate PUSH delta (Target -> Pushed Position)
        // If dragging right, we push target further right? No, if I drag A onto B (A is Left of B), I want to push B to the Right.
        // If I drag A onto B (A is Right of B), I want to push B to the Left.
        // Actually, the user drags A *onto* B. 
        // If A was left of B, and I drag A right onto B, I want B to move Right (to empty space).
        // If A was right of B, and I drag A left onto B, I want B to move Left.
        
        // So the push direction is roughly the same as the drag direction.
        var pushDeltaX = 0f
        var pushDeltaY = 0f
        
        // We need to push by roughly the size of the moved group (or target group?)
        // Usually grid size. Let's estimate grid size from piece width.
        val pieceSizeX = pieces[0].width.toFloat()
        val pieceSizeY = pieces[0].height.toFloat()
        
        if (isHorizontalDrag) {
            if (dragDx > 0) pushDeltaX = pieceSizeX * (movedWidth / pieceSizeX).coerceAtLeast(1f) // Push Right
            else pushDeltaX = -pieceSizeX * (movedWidth / pieceSizeX).coerceAtLeast(1f) // Push Left
        } else {
            if (dragDy > 0) pushDeltaY = pieceSizeY * (movedHeight / pieceSizeY).coerceAtLeast(1f) // Push Down
            else pushDeltaY = -pieceSizeY * (movedHeight / pieceSizeY).coerceAtLeast(1f) // Push Up
        }
        
        // Check if PUSH is valid (Target moves to Target + PushDelta)
        // REPLACED: val canPush = isPositionValid(targetPieces, pushDeltaX, pushDeltaY)
        // WITH: Recursive Chain Push logic
        
        val pushTransaction = mutableMapOf<PuzzlePiece, Pair<Float, Float>>()
        val visitedGroups = mutableSetOf<Int>()
        
        // Pre-add the moved group to transaction (with 0 delta) so they are considered "moving"
        // and don't block the push (though they are physically at the push origin)
        movedGroup.forEach { pushTransaction[it] = Pair(0f, 0f) }
        
        val canPush = tryChainPush(targetPieces, pushDeltaX, pushDeltaY, pushTransaction, visitedGroups, 0)
        
        if (canPush) {
            Log.d(TAG, "checkSwap: CHAIN PUSH detected! Moving ${pushTransaction.size - movedGroup.size} pieces.")
            
            // Apply all moves in transaction (except movedGroup which has 0 delta)
            pushTransaction.forEach { (piece, delta) ->
                if (delta.first != 0f || delta.second != 0f) {
                    piece.currentX += delta.first
                    piece.currentY += delta.second
                }
            }
            
            // Reset Z-indices
            pieces.forEachIndexed { index, piece -> piece.zIndex = index }
            pieces.sortBy { it.zIndex }
            
            return true // Push successful, no need to proceed to Swap
        } else {
            Log.d(TAG, "checkSwap: Push invalid (blocked/out of bounds), falling back to SWAP.")
        }
        
        // If Push failed, we fall back to SWAP logic below...
        
        val deltaX_TtoNew = deltaX_Target
        val deltaY_TtoNew = deltaY_Target
        
        // --- BOUNDARY CHECK START ---

        // Verify if the move would put any piece out of bounds
        val gameWidth = width.toFloat()
        val gameHeight = height.toFloat()
        val padding = 20f // Keep pieces slightly away from the absolute edge
        // Note: We removed the extra bottomPadding constraint for swap logic to be symmetric
        
        // Simulate moved group at new position
        for (piece in movedGroup) {
            val newX = piece.currentX + deltaX_MtoT
            val newY = piece.currentY + deltaY_MtoT
            // STRICT BOUNDARY CHECK for Swap:
            // NO TOLERANCE. Must be strictly inside puzzleBounds.
            // Floating point errors might need a tiny epsilon (e.g. 1f), but effectively 0.
            
            // Use puzzleBounds instead of gameWidth/Height for strict playable area compliance
            // Or at least ensure it doesn't go off screen.
            // Let's use puzzleBounds for strictness if available, or 0..width/height
            
            // Since we already clamp drag to puzzleBounds, swap should also respect it.
            // But puzzleBounds is private and calculated in updateLayout.
            // Let's assume puzzleBounds is the playable area.
            
            val tolerance = 1f // Minimal tolerance for float rounding
            
            if (newX < puzzleBounds.left - tolerance || 
                newY < puzzleBounds.top - tolerance || 
                newX + piece.width > puzzleBounds.right + tolerance || 
                newY + piece.height > puzzleBounds.bottom + tolerance) {
                
                Log.d(TAG, "checkSwap: ABORT - Moved piece[${piece.id}] would be out of bounds at ($newX, $newY)")
                return false
            }
        }
        
        // Simulate target group at new position (Either Origin or Pushed)
        for (piece in targetPieces) {
            val newX = piece.currentX + deltaX_TtoNew
            val newY = piece.currentY + deltaY_TtoNew
            
            val tolerance = 1f
            if (newX < puzzleBounds.left - tolerance || 
                newY < puzzleBounds.top - tolerance || 
                newX + piece.width > puzzleBounds.right + tolerance || 
                newY + piece.height > puzzleBounds.bottom + tolerance) {
                
                Log.d(TAG, "checkSwap: ABORT - Target piece[${piece.id}] would be out of bounds at ($newX, $newY)")
                return false
            }
        }
        // --- BOUNDARY CHECK END ---
        
        // --- COLLISION CHECK START ---
        // Verify if the move would cause overlap with any other pieces (stationary or moving)
        // This prevents swapping groups of different sizes into spaces that don't fit
        
        val allMovingPieces = movedGroup + targetPieces
        val stationaryPieces = pieces.filter { !allMovingPieces.contains(it) }
        
        // Define new positions for moving pieces
        val newPositions = mutableListOf<Pair<PuzzlePiece, android.graphics.RectF>>()
        
        movedGroup.forEach { piece ->
            val newX = piece.currentX + deltaX_MtoT
            val newY = piece.currentY + deltaY_MtoT
            // Shrink slightly to avoid touching edge collisions
            // Using a larger shrink value (e.g., 5f) makes it more forgiving for "squeezing"
            newPositions.add(Pair(piece, android.graphics.RectF(newX + 5, newY + 5, newX + piece.width - 5, newY + piece.height - 5)))
        }
        
        targetPieces.forEach { piece ->
            val newX = piece.currentX + deltaX_TtoNew
            val newY = piece.currentY + deltaY_TtoNew
            newPositions.add(Pair(piece, android.graphics.RectF(newX + 5, newY + 5, newX + piece.width - 5, newY + piece.height - 5)))
        }
        
        // Check for collisions
        for ((pieceA, rectA) in newPositions) {
            // 1. Check against stationary pieces
            for (stationary in stationaryPieces) {
                // RELAXED COLLISION CHECK for Swap:
                // We need to be more forgiving here. If the user is dragging a big block,
                // and it *slightly* overlaps with a stationary neighbor that isn't the swap target,
                // we should ignore it if the overlap is minimal (just "grazing").
                
                // Increase shrink value significantly for stationary obstacles during swap check.
                // This acts as a "lubricant" for sliding past neighbors.
                val shrinkAmount = 10f 
                val rectB = android.graphics.RectF(
                    stationary.currentX + shrinkAmount, 
                    stationary.currentY + shrinkAmount, 
                    stationary.currentX + stationary.width - shrinkAmount, 
                    stationary.currentY + stationary.height - shrinkAmount
                )
                
                if (android.graphics.RectF.intersects(rectA, rectB)) {
                    Log.d(TAG, "checkSwap: ABORT - Collision detected between moving piece[${pieceA.id}] and stationary piece[${stationary.id}]")
                    return false
                }
            }
            
            // 2. Check against other moving pieces
            for ((pieceB, rectB) in newPositions) {
                if (pieceA.id == pieceB.id) continue // Skip self
                
                // Internal collision within moving set should be strict or also relaxed?
                // Since we move them relative to each other based on grid, they shouldn't overlap unless logic is wrong.
                // But let's keep it strict-ish (5f) as before.
                if (android.graphics.RectF.intersects(rectA, rectB)) {
                     Log.d(TAG, "checkSwap: ABORT - Collision detected between moving piece[${pieceA.id}] and moving piece[${pieceB.id}]")
                     return false
                }
            }
        }
        // --- COLLISION CHECK END ---
        
        Log.d(TAG, "  Moved group current bounds: ($movedMinX, $movedMinY) to ($movedMaxX, $movedMaxY)")
        Log.d(TAG, "  Moved group original bounds: ($movedGroupOriginalMinX, $movedGroupOriginalMinY) to ($movedGroupOriginalMaxX, $movedGroupOriginalMaxY)")
        Log.d(TAG, "  Target bounds: ($targetMinX, $targetMinY) to ($targetMaxX, $targetMaxY)")
        Log.d(TAG, "  Moving moved group by delta=($deltaX_MtoT, $deltaY_MtoT)")
        Log.d(TAG, "  Moving target pieces by delta=($deltaX_TtoNew, $deltaY_TtoNew)")
        
        // Apply moves to moved group
        movedGroup.forEach { 
            val oldX = it.currentX
            val oldY = it.currentY
            it.currentX += deltaX_MtoT
            it.currentY += deltaY_MtoT
            Log.d(TAG, "    Moved piece[${it.id}] from ($oldX, $oldY) to (${it.currentX}, ${it.currentY})")
        }
        
        // Apply moves to all target pieces
        targetPieces.forEach { 
            val oldX = it.currentX
            val oldY = it.currentY
            it.currentX += deltaX_TtoNew
            it.currentY += deltaY_TtoNew
            Log.d(TAG, "    Target piece[${it.id}] from ($oldX, $oldY) to (${it.currentX}, ${it.currentY})")
        }
        
        
        // REMOVED: Detach target pieces logic. 
        // We now swap the ENTIRE group, so no need to detach.
        // The group structure is preserved.
        
        // Reset Z-indices
        resetZIndex()
        
        return true
    }
    */

    private fun isValidPlacement(group: List<PuzzlePiece>): Boolean {
        // 基于网格的有效性检查
        val currentCells = getCurrentGridPosition(group)
        val excludeGroups = setOf(group[0].groupId)
        
        val isInBounds = areGridCellsInBounds(currentCells)
        val isFree = areGridCellsFree(currentCells, excludeGroups)
        
        if (!isInBounds) {
            Log.d(TAG, "isValidPlacement: Out of bounds")
        }
        if (!isFree) {
            Log.d(TAG, "isValidPlacement: Cells occupied")
        }
        
        return isInBounds && isFree
    }

    private fun checkForWin() {
        if (pieces.isEmpty()) return
        
        val firstGroupId = pieces[0].groupId
        val allSameGroup = pieces.all { it.groupId == firstGroupId }
        
        if (allSameGroup && !particleSystem.isFireworksMode) {
            particleSystem.isFireworksMode = true
            isTimerRunning = false
            invalidate()
            // Notify completion with elapsed time (ensure on main thread)
            post {
                onPuzzleComplete?.invoke(elapsedSeconds)
            }
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
            
            Log.d(TAG, "checkNeighborSnapping: Merged total $totalMergedEdges edges. Triggering celebration.")
            
            // Bring merged group to front
            bringGroupToFront(movedPiece.groupId)
        }
        
        if (hasMergedAny) {
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
    /*
    private fun tryChainPush(
        groupToPush: List<PuzzlePiece>,
        dx: Float,
        dy: Float,
        transaction: MutableMap<PuzzlePiece, Pair<Float, Float>>,
        visitedGroups: MutableSet<Int>,
        depth: Int
    ): Boolean {
        if (depth > 10) return false // Limit recursion depth
        if (groupToPush.isEmpty()) return true

        // 1. Check Bounds
        // Use strict bounds similar to isValidPlacement
        val gameWidth = width.toFloat()
        val gameHeight = height.toFloat()
        // Use puzzleBounds if available (but it's private member, we can access it)
        // puzzleBounds is member of GameBoardView
        
        for (piece in groupToPush) {
            val newX = piece.currentX + dx
            val newY = piece.currentY + dy
            
            // Strict check
            if (newX < puzzleBounds.left - 1f || 
                newY < puzzleBounds.top - 1f || 
                newX + piece.width > puzzleBounds.right + 1f || 
                newY + piece.height > puzzleBounds.bottom + 1f) {
                // Log.d(TAG, "tryChainPush: Out of bounds at depth $depth")
                return false
            }
        }

        // 2. Find Obstacles
        val obstacles = mutableSetOf<PuzzlePiece>()
        
        for (piece in groupToPush) {
            // Predict new position
            val newRect = android.graphics.RectF(
                piece.currentX + dx + 2, 
                piece.currentY + dy + 2, 
                piece.currentX + piece.width + dx - 2, 
                piece.currentY + piece.height + dy - 2
            )
            
            for (other in pieces) {
                if (other in groupToPush) continue
                if (transaction.containsKey(other)) continue // Already moving in this transaction
                
                // Check overlap
                val otherRect = android.graphics.RectF(
                    other.currentX + 2, 
                    other.currentY + 2, 
                    other.currentX + other.width - 2, 
                    other.currentY + other.height - 2
                )
                
                if (android.graphics.RectF.intersects(newRect, otherRect)) {
                    obstacles.add(other)
                }
            }
        }

        // Add current group to transaction
        groupToPush.forEach { transaction[it] = Pair(dx, dy) }
        
        if (obstacles.isEmpty()) {
            return true
        }

        // 3. Handle Obstacles (Recursion)
        val obstacleGroupIds = obstacles.map { it.groupId }.toSet()
        
        // Check for cycles or already visited groups in this push chain
        if (obstacleGroupIds.any { it in visitedGroups }) return false
        visitedGroups.addAll(obstacleGroupIds)
        
        val nextPiecesToPush = pieces.filter { it.groupId in obstacleGroupIds }
        
        // Recursive call: Try to push the obstacles in the SAME direction
        return tryChainPush(nextPiecesToPush, dx, dy, transaction, visitedGroups, depth + 1)
    }
    */

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
            Log.d(TAG, "moveGroupToGridCells: Size mismatch (${group.size} pieces vs ${targetCells.size} cells)")
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
                
                Log.d(TAG, "  Moved piece[${piece.id}] to grid (${targetCell.row}, ${targetCell.col}) at pixel (${piece.currentX}, ${piece.currentY})")
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
            Log.d(TAG, "mergeGroups: Awarded $mergedEdges points for merging groups $sourceGroupId+$targetGroupId -> $targetGroupId (new edges: $mergedEdges, group total: $newTotalScore)")
        }
        
        return mergedEdges
    }

}

