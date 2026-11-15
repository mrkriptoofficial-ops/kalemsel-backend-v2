package com.gezgin.zikirmatik

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.ZonedDateTime

// ───────────── Hatırlatıcılar ─────────────
object Reminders {
    private const val CH = "zikir_hatirlatma"

    fun schedule(ctx: Context, s: Settings) {
        one(ctx, 1, s.remMorning, s.morningMin)
        one(ctx, 2, s.remEvening, s.eveningMin)
    }

    private fun pending(ctx: Context, code: Int): PendingIntent = PendingIntent.getBroadcast(
        ctx, code,
        Intent(ctx, ReminderReceiver::class.java).putExtra("code", code),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun one(ctx: Context, code: Int, on: Boolean, minutes: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(ctx, code)
        am.cancel(pi)
        if (!on) return
        val now = ZonedDateTime.now()
        var t = now.withHour(minutes / 60).withMinute(minutes % 60).withSecond(0).withNano(0)
        if (!t.isAfter(now.plusSeconds(20))) t = t.plusDays(1)
        am.setWindow(AlarmManager.RTC_WAKEUP, t.toInstant().toEpochMilli(), 5 * 60 * 1000L, pi)
    }

    fun notify(ctx: Context, code: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(NotificationChannel(CH, "Zikir hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(
            ctx, 10 + code, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (code == 1) "Sabah zikri vakti" else "Akşam zikri vakti"
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFD4AE62.toInt())
            .setContentTitle(title)
            .setContentText("Kalpler ancak Allah'ı anmakla huzur bulur.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(code, n)
        } catch (_: SecurityException) {
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val code = intent.getIntExtra("code", 1)
        Reminders.notify(ctx, code)
        Reminders.schedule(ctx, Repo(ctx).loadSettings())
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.schedule(ctx, Repo(ctx).loadSettings())
        ZikirWidget.refresh(ctx)
    }
}

// ───────────── Ana ekran widget'ı ─────────────
class ZikirWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val v = views(ctx)
        ids.forEach { mgr.updateAppWidget(it, v) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == ACTION_INC) {
            val repo = Repo(ctx)
            val z = repo.widgetIncrement()
            val s = repo.loadSettings()
            if (z != null && s.haptic > 0) {
                val vib = ctx.systemVibrator()
                if (vib.hasVibrator()) {
                    if (z.count > 0 && z.count % z.target == 0) vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 70, 60, 70, 220), -1))
                    else if (Build.VERSION.SDK_INT >= 29) vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    else vib.vibrate(VibrationEffect.createOneShot(14, 150))
                }
            }
            refresh(ctx)
        }
    }

    companion object {
        const val ACTION_INC = "com.gezgin.zikirmatik.WIDGET_INC"

        fun refresh(ctx: Context) {
            val m = AppWidgetManager.getInstance(ctx) ?: return
            val ids = m.getAppWidgetIds(ComponentName(ctx, ZikirWidget::class.java))
            if (ids.isEmpty()) return
            val v = views(ctx)
            ids.forEach { m.updateAppWidget(it, v) }
        }

        private fun views(ctx: Context): RemoteViews {
            val repo = Repo(ctx)
            val list = repo.loadZikirs()
            val z = list.firstOrNull { it.id == repo.selId } ?: list.first()
            val s = repo.loadSettings()
            val shown = when {
                s.stopAtTarget -> z.count.coerceAtMost(z.target)
                z.count > 0 && z.count % z.target == 0 -> z.target
                else -> z.count % z.target
            }
            return RemoteViews(ctx.packageName, R.layout.widget_zikir).apply {
                setTextViewText(R.id.w_name, z.okunus)
                setTextViewText(R.id.w_count, shown.toString())
                setTextViewText(R.id.w_target, "/ ${z.target}")
                setOnClickPendingIntent(
                    R.id.w_tap,
                    PendingIntent.getBroadcast(
                        ctx, 0, Intent(ctx, ZikirWidget::class.java).setAction(ACTION_INC),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                setOnClickPendingIntent(
                    R.id.w_open,
                    PendingIntent.getActivity(
                        ctx, 1, Intent(ctx, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
        }
    }
}
