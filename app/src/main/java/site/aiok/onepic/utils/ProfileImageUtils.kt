package site.aiok.onepic.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ProfileImageUtils {
    private const val PROFILES_DIR = "profiles"
    private const val AVATAR_FILE_NAME = "avatar_v1.webp"

    fun saveAvatarFromUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val profilesDir = File(context.filesDir, PROFILES_DIR)
            if (!profilesDir.exists()) {
                profilesDir.mkdirs()
            }
            
            val avatarFile = File(profilesDir, AVATAR_FILE_NAME)
            val outputStream = FileOutputStream(avatarFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            avatarFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAvatarFile(context: Context): File? {
        val file = File(File(context.filesDir, PROFILES_DIR), AVATAR_FILE_NAME)
        return if (file.exists()) file else null
    }
}
