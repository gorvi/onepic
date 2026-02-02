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
    val cols: Int,            // Number of columns to slice
    val storyText: String? = null,
    val animationTheme: String? = null,
    val isAscended: Boolean = false,
    // Project Exodus Fields
    val moduleName: String? = null,
    val integrityStatus: String? = null, // e.g. "Quantum Keel: 10% Integrity"
    val motivation: String? = null
)
