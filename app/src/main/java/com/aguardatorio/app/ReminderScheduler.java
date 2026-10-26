package com.aguardatorio.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

final class ReminderScheduler {
    static final String KIND_WATER = "water";
    static final String KIND_PILLS = "pills";
    static final String CHANNEL_ID = "recordatorios";
    static final String PREFS = "recordatorios";
    static final String KEY_ACTIVE = "active";
    static final String KEY_INTERVAL = "interval_hours";
    static final String KEY_KIND = "kind";
    static final String KEY_NEXT_ALARM = "next_alarm";
    static final int MIN_INTERVAL = 1;
    static final int MAX_INTERVAL = 12;
    static final int DEFAULT_INTERVAL = 2;
    private static final int ALARM_REQUEST_CODE = 101;
    private static final long[] VIBRATION_PATTERN = {0, 300, 150, 450};

    private ReminderScheduler() {}

    static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Agua y pastillas",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Avisos para hidratarte o recordar tus pastillas");
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATION_PATTERN);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    static void scheduleNext(Context context) {
        var preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(KEY_ACTIVE, false)) {
            cancel(context);
            return;
        }

        int hours = preferences.getInt(KEY_INTERVAL, DEFAULT_INTERVAL);
        long triggerAt = System.currentTimeMillis() + hours * AlarmManager.INTERVAL_HOUR;
        preferences.edit().putLong(KEY_NEXT_ALARM, triggerAt).apply();

        AlarmManager manager = context.getSystemService(AlarmManager.class);
        manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                reminderPendingIntent(context)
        );
    }

    static void cancel(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        manager.cancel(reminderPendingIntent(context));
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_NEXT_ALARM)
                .apply();
    }

    static PendingIntent reminderPendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    static void deliverReminder(Context context) {
        var preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!preferences.getBoolean(KEY_ACTIVE, false)) {
            return;
        }

        boolean isWater = KIND_WATER.equals(preferences.getString(KEY_KIND, KIND_WATER));
        String title = isWater ? "Toma agua" : "Hora de tus pastillas";
        String message = isWater
                ? "El calor aprieta. Hidrátate con un poco de agua."
                : "Es hora de tus pastillas. Sigue la dosis indicada.";

        createNotificationChannel(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_drop)
                .setContentTitle(title)
                .setContentText(message)
                .setCategory(android.app.Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent(context))
                .build();
        manager.notify(201, notification);

        scheduleNext(context);
    }

    static void showTestNotification(Context context) {
        createNotificationChannel(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        android.app.Notification notification = new android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_drop)
                .setContentTitle("¡Vibración lista!")
                .setContentText("Así se sentirá tu recordatorio.")
                .setCategory(android.app.Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent(context))
                .build();
        manager.notify(202, notification);
    }

    private static PendingIntent openAppPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(
                context,
                102,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
