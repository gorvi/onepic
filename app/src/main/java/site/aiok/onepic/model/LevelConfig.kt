package site.aiok.onepic.model

import android.net.Uri

sealed class ImageSource {
    data class Resource(val resId: Int) : ImageSource()
    data class Asset(val path: String) : ImageSource()
    data class UriSource(val uri: Uri) : ImageSource()
    object Generated : ImageSource()
}

data class LevelConfig(
    val levelId: String,
    val title: String,
    val difficulty: String,  // "Easy", "Medium", "Hard"
    val imageSource: ImageSource,
    val rows: Int,           // Number of rows to slice
    val cols: Int            // Number of columns to slice
)
