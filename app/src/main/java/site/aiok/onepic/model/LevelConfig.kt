package site.aiok.onepic.model

data class LevelConfig(
    val levelId: Int,
    val title: String,
    val difficulty: String,  // "Easy", "Medium", "Hard"
    val imageResId: Int,     // Resource ID of the original image (or 0 for generated)
    val rows: Int,           // Number of rows to slice
    val cols: Int            // Number of columns to slice
)
