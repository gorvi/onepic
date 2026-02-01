package site.aiok.onepic.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import site.aiok.onepic.data.LevelProgressManager

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "site.aiok.onepic.DAILY_REMINDER") {
            // 只有在用户开启了提醒逻辑时才显示
            if (LevelProgressManager.isDailyReminderEnabled(context)) {
                val slotIndex = intent.getIntExtra(NotificationHelper.EXTRA_SLOT_INDEX, -1)
                NotificationHelper.showNotification(context, slotIndex)
            }
        }
    }
}
