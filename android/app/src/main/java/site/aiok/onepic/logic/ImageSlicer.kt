
package site.aiok.onepic.logic

import android.graphics.Bitmap
import site.aiok.onepic.model.PuzzlePiece
import kotlin.random.Random

object ImageSlicer {

    /**
     * 切分图片为拼图块，并洗牌到随机位置
     * 注意：这里只负责切图和生成块，不设置像素坐标
     * 像素坐标由 GameBoardView.updateLayout() 根据屏幕尺寸计算
     */
    fun sliceImage(originalBitmap: Bitmap, rows: Int, cols: Int): List<PuzzlePiece> {
        val pieces = mutableListOf<PuzzlePiece>()
        val pieceWidth = originalBitmap.width / cols
        val pieceHeight = originalBitmap.height / rows

        var idCounter = 0
        
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // 原始位置（用于切图）
                val x = col * pieceWidth
                val y = row * pieceHeight

                // 创建此块的 bitmap
                val pieceBitmap = Bitmap.createBitmap(originalBitmap, x, y, pieceWidth, pieceHeight)

                pieces.add(
                    PuzzlePiece(
                        id = idCounter,
                        currentX = 0f, // 将在 updateLayout 中设置
                        currentY = 0f,
                        targetX = 0f,  // 将在 updateLayout 中设置
                        targetY = 0f,
                        width = pieceWidth.toFloat(),
                        height = pieceHeight.toFloat(),
                        bitmap = pieceBitmap,
                        zIndex = 0,
                        groupId = idCounter, // 每块初始独立
                        row = row,           // 目标行（正确位置）
                        col = col            // 目标列（正确位置）
                    )
                )
                idCounter++
            }
        }
        
        return pieces
    }
}
