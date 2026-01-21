package site.aiok.onepic.audio

import android.content.Context
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 音效管理器
 * 支持从 assets 或下载的文件加载音效
 */
class SoundManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "SoundManager"
        private const val MAX_STREAMS = 5 // 同时播放的最大音效数
        
        @Volatile
        private var INSTANCE: SoundManager? = null
        
        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>() // 音效类型 -> SoundPool ID
    private var isEnabled = true // 音效开关
    
    init {
        initializeSoundPool()
        loadSounds()
    }
    
    private fun initializeSoundPool() {
        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .build()
    }
    
    /**
     * 从 assets 加载音效文件
     */
    private fun loadSounds() {
        soundPool?.let { pool ->
            try {
                // 从 assets/sounds/ 目录加载音效文件
                soundMap[SoundType.SNAP] = loadSoundFromAssets("sounds/snap.mp3", pool)
                soundMap[SoundType.SWAP] = loadSoundFromAssets("sounds/swap.mp3", pool)
                soundMap[SoundType.COMPLETE] = loadSoundFromAssets("sounds/complete.mp3", pool)
                soundMap[SoundType.REVERT] = loadSoundFromAssets("sounds/revert.mp3", pool)
                soundMap[SoundType.PUSH] = loadSoundFromAssets("sounds/push.mp3", pool)
            } catch (e: Exception) {
                Log.w(TAG, "加载音效失败: ${e.message}")
            }
        }
    }
    
    /**
     * 从 assets 加载音效文件
     * @return SoundPool ID，如果加载失败返回 0
     */
    private fun loadSoundFromAssets(fileName: String, pool: SoundPool): Int {
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open(fileName)
            val tempFile = File(context.cacheDir, fileName.substringAfterLast("/"))
            
            // 将 assets 中的文件复制到临时文件
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            // 从临时文件加载到 SoundPool
            pool.load(tempFile.absolutePath, 1)
        } catch (e: Exception) {
            Log.d(TAG, "音效文件不存在或加载失败: $fileName")
            0 // 返回 0 表示加载失败
        }
    }
    
    /**
     * 播放音效
     * @param soundType 音效类型
     * @param volume 音量 (0.0f - 1.0f)，默认 0.7f
     */
    fun playSound(soundType: SoundType, volume: Float = 0.7f) {
        if (!isEnabled) return
        
        val soundId = soundMap[soundType] ?: 0
        if (soundId == 0) {
            // 音效未加载，静默跳过
            return
        }
        
        soundPool?.play(
            soundId,
            volume, // 左声道音量
            volume, // 右声道音量
            1, // 优先级
            0, // 不循环
            1f // 播放速度
        )
    }
    
    /**
     * 设置音效开关
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * 检查音效是否启用
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}

/**
 * 音效类型枚举
 */
enum class SoundType {
    SNAP,      // 拼图块合并/吸附
    SWAP,      // 拼图块交换
    COMPLETE,  // 拼图完成
    REVERT,    // 拼图块回退
    PUSH       // 拼图块推挤
}
