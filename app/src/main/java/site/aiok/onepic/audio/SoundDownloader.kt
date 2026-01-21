package site.aiok.onepic.audio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 音效下载器
 * 负责从网络下载音效文件
 */
object SoundDownloader {
    private const val TAG = "SoundDownloader"
    private const val SOUNDS_DIR = "sounds"
    
    // 音效文件 URL
    // 使用 Mixkit 的免费音效资源（无需注册，可直接使用）
    // 如果这些 URL 不可用，可以替换为其他免费音效资源或自己的服务器
    private val SOUND_URLS = mapOf(
        // 点击/轻触音效
        SoundType.SNAP to "https://assets.mixkit.co/sfx/download/mixkit-game-ball-tap-2073.mp3",
        SoundType.SWAP to "https://assets.mixkit.co/sfx/download/mixkit-game-ball-tap-2073.mp3",
        // 成功/完成音效
        SoundType.COMPLETE to "https://assets.mixkit.co/sfx/download/mixkit-achievement-bell-600.mp3",
        // 回退音效
        SoundType.REVERT to "https://assets.mixkit.co/sfx/download/mixkit-game-ball-tap-2073.mp3",
        // 推挤音效
        SoundType.PUSH to "https://assets.mixkit.co/sfx/download/mixkit-game-ball-tap-2073.mp3"
    )
    
    // 注意：实际使用时，建议：
    // 1. 将音效文件上传到自己的 GitHub 仓库，使用 raw 链接
    // 2. 或使用自己的 CDN/服务器
    // 3. 或使用其他可靠的免费音效资源网站
    
    // 使用 GitHub raw 或其他免费 CDN 的示例 URL 格式
    // 实际使用时，可以将音效文件上传到 GitHub 仓库，然后使用 raw 链接
    // 例如：https://raw.githubusercontent.com/username/repo/main/sounds/snap.mp3
    
    /**
     * 检查网络连接
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return connectivityManager?.let { cm ->
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } ?: false
    }
    
    /**
     * 下载所有缺失的音效文件
     * @return 成功下载的文件数量
     */
    suspend fun downloadMissingSounds(context: Context): Int = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "网络不可用，跳过音效下载")
            return@withContext 0
        }
        
        val soundsDir = File(context.filesDir, SOUNDS_DIR)
        if (!soundsDir.exists()) {
            soundsDir.mkdirs()
        }
        
        var downloadedCount = 0
        
        SoundType.values().forEach { soundType ->
            val fileName = getFileName(soundType)
            val localFile = File(soundsDir, fileName)
            
            // 如果文件已存在，跳过
            if (localFile.exists() && localFile.length() > 0) {
                Log.d(TAG, "音效文件已存在: $fileName")
                return@forEach
            }
            
            // 尝试下载
            val url = SOUND_URLS[soundType]
            if (url != null && downloadFile(url, localFile)) {
                downloadedCount++
                Log.i(TAG, "成功下载音效: $fileName")
            } else {
                Log.w(TAG, "下载音效失败: $fileName")
            }
        }
        
        downloadedCount
    }
    
    /**
     * 下载单个音效文件
     */
    private suspend fun downloadFile(urlString: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: java.io.InputStream? = null
        var outputStream: FileOutputStream? = null
        
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000 // 10秒超时
            connection.readTimeout = 10000
            connection.requestMethod = "GET"
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "下载失败，HTTP状态码: ${connection.responseCode}")
                return@withContext false
            }
            
            inputStream = connection.inputStream
            outputStream = FileOutputStream(targetFile)
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "下载文件异常: ${e.message}", e)
            // 删除可能不完整的文件
            if (targetFile.exists()) {
                targetFile.delete()
            }
            false
        } finally {
            inputStream?.close()
            outputStream?.close()
            connection?.disconnect()
        }
    }
    
    /**
     * 获取音效文件名
     */
    fun getFileName(soundType: SoundType): String {
        return when (soundType) {
            SoundType.SNAP -> "snap.mp3"
            SoundType.SWAP -> "swap.mp3"
            SoundType.COMPLETE -> "complete.mp3"
            SoundType.REVERT -> "revert.mp3"
            SoundType.PUSH -> "push.mp3"
        }
    }
    
    /**
     * 获取音效文件的本地路径
     */
    fun getLocalFilePath(context: Context, soundType: SoundType): File {
        val soundsDir = File(context.filesDir, SOUNDS_DIR)
        return File(soundsDir, getFileName(soundType))
    }
    
    /**
     * 检查音效文件是否存在
     */
    fun isSoundFileExists(context: Context, soundType: SoundType): Boolean {
        val file = getLocalFilePath(context, soundType)
        return file.exists() && file.length() > 0
    }
}
