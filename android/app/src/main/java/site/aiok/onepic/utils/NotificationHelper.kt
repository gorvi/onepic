package site.aiok.onepic.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import site.aiok.onepic.MainActivity
import site.aiok.onepic.R
import java.util.Calendar

object NotificationHelper {
    private const val CHANNEL_ID = "daily_reminder_channel"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_CODE = 2001
    
    const val ACTION_REMINDER = "site.aiok.onepic.DAILY_REMINDER"
    const val EXTRA_SLOT_INDEX = "extra_slot_index" // 用于追踪点击了哪个时段

    fun scheduleDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 定义三个大数据推荐时段: 9:00 (早晨), 12:30 (午休), 20:00 (晚间黄金时段)
        val slots = listOf(
            Pair(9, 0),
            Pair(12, 30),
            Pair(20, 0)
        )
        
        // 调度逻辑：优先读取用户偏好，如果没有偏好再随机
        val preferredIndex = site.aiok.onepic.data.LevelProgressManager.getPreferredReminderSlot(context)
        val selectedIndex = if (preferredIndex in slots.indices) preferredIndex else slots.indices.random()
        val (randomHour, randomMinute) = slots[selectedIndex]

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_SLOT_INDEX, selectedIndex)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, randomHour)
            set(Calendar.MINUTE, randomMinute)
            set(Calendar.SECOND, 0)
            
            // 如果计算出的时间已经过了今天，则设为明天
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showNotification(context: Context, slotIndex: Int = -1) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.daily_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.daily_reminder_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 随机选择话术 (1-3)
        val randomIndex = (1..3).random()
        val titleResId = when(randomIndex) {
            1 -> R.string.daily_reminder_title_1
            2 -> R.string.daily_reminder_title_2
            else -> R.string.daily_reminder_title_3
        }
        val msgResId = when(randomIndex) {
            1 -> R.string.daily_reminder_msg_1
            2 -> R.string.daily_reminder_msg_2
            else -> R.string.daily_reminder_msg_3
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // 确保这里有小图标资源
            .setContentTitle(context.getString(titleResId))
            .setContentText(context.getString(msgResId))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
        
        // 发送完后通过递归或者重新设置系统闹铃来维持“每日”循环
        // 虽然有 setInexactRepeating，但在现代 Android 上为了省电，精确闹钟单次设置更可靠
        scheduleDailyReminder(context)
    }
}
