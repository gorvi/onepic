package site.aiok.onepic.model

import android.graphics.Bitmap

data class PuzzlePiece(
    val id: Int,
    var currentX: Float,
    var currentY: Float,
    var targetX: Float,
    var targetY: Float,
    var width: Float,
    var height: Float,
    var bitmap: Bitmap,
    var zIndex: Int,
    var isLocked: Boolean = false,
    var groupId: Int = -1,
    var row: Int,  // 改为 var 以支持网格位置更新
    var col: Int,  // 改为 var 以支持网格位置更新
)
