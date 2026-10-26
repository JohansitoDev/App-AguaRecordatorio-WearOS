package com.aguardatorio.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 31;
    private static final int COLOR_BACKGROUND = Color.rgb(16, 21, 17);
    private static final int COLOR_CARD = Color.rgb(29, 37, 31);
    private static final int COLOR_ACCENT = Color.rgb(66, 217, 181);
    private static final int COLOR_WHITE = Color.rgb(243, 247, 242);
    private static final int COLOR_MUTED = Color.rgb(158, 174, 164);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingPermissionAction;
    private TextView statusView;
    private TextView intervalView;
    private TextView countdownView;
    private TextView reminderTitleView;
    private TextView descriptionView;
    private Button waterButton;
    private Button pillsButton;
    private Button startButton;
    private int intervalHours;
    private boolean isActive;
    private String kind;

    private final Runnable refreshCountdown = new Runnable() {
        @Override
        public void run() {
            if (countdownView != null) {
                countdownView.setText(getCountdown());
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BACKGROUND);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);

        ReminderScheduler.createNotificationChannel(this);
        readSettings();
        buildScreen();
        updateScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        readSettings();
        if (startButton != null) {
            updateScreen();
        }
        handler.removeCallbacks(refreshCountdown);
        handler.post(refreshCountdown);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshCountdown);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST) {
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Runnable action = pendingPermissionAction;
            pendingPermissionAction = null;
            if (action != null) {
                action.run();
            }
        } else {
            pendingPermissionAction = null;
            statusView.setText(R.string.notification_permission_denied);
        }
    }

    private void readSettings() {
        var preferences = getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE);
        isActive = preferences.getBoolean(ReminderScheduler.KEY_ACTIVE, false);
        intervalHours = preferences.getInt(
                ReminderScheduler.KEY_INTERVAL,
                ReminderScheduler.DEFAULT_INTERVAL
        );
        kind = preferences.getString(ReminderScheduler.KEY_KIND, ReminderScheduler.KIND_WATER);
    }

    private void buildScreen() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToOutline(true);
        scrollView.setBackgroundColor(COLOR_BACKGROUND);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        boolean isRound = getResources().getConfiguration().isScreenRound();
        int horizontalPadding = isRound ? dp(32) : dp(24);
        int verticalPadding = isRound ? dp(26) : dp(16);
        content.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

        buildModeSelector(content);

        reminderTitleView = text("", 23, COLOR_WHITE, true);
        reminderTitleView.setGravity(Gravity.CENTER);
        reminderTitleView.setPadding(0, dp(3), 0, 0);
        content.addView(reminderTitleView, wrapCentered());

        descriptionView = text("", 12, COLOR_MUTED, false);
        descriptionView.setGravity(Gravity.CENTER);
        content.addView(descriptionView, wrapCentered());

        LinearLayout countdownRow = new LinearLayout(this);
        countdownRow.setGravity(Gravity.CENTER);
        TextView nextLabel = text("PRÓXIMO AVISO", 9, COLOR_MUTED, true);
        nextLabel.setLetterSpacing(0.12f);
        countdownRow.addView(nextLabel, new LinearLayout.LayoutParams(-2, -2));

        countdownView = text("", 25, COLOR_ACCENT, true);
        countdownView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams countdownParams = new LinearLayout.LayoutParams(-2, -2);
        countdownParams.leftMargin = dp(9);
        countdownRow.addView(countdownView, countdownParams);
        LinearLayout.LayoutParams countdownRowParams = new LinearLayout.LayoutParams(-1, dp(34));
        countdownRowParams.topMargin = dp(2);
        content.addView(countdownRow, countdownRowParams);

        addIntervalControl(content);

        statusView = text("", 9, COLOR_MUTED, false);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 0, 0, dp(2));
        content.addView(statusView, wrapCentered());

        startButton = button("", COLOR_ACCENT, COLOR_BACKGROUND, 13);
        content.addView(startButton, buttonLayoutParams(42));
        startButton.setOnClickListener(view -> requestPermissionThen(this::toggleReminders));

        Button testButton = button("PROBAR VIBRACIÓN", COLOR_BACKGROUND, COLOR_WHITE, 9);
        testButton.setBackground(background(COLOR_BACKGROUND, COLOR_MUTED, dp(18), dp(1)));
        testButton.setOnClickListener(view -> requestPermissionThen(this::testVibration));
        LinearLayout.LayoutParams testParams = buttonLayoutParams(34);
        testParams.topMargin = dp(4);
        content.addView(testButton, testParams);

        scrollView.addView(content);
        setContentView(scrollView);
    }

    private void buildModeSelector(LinearLayout parent) {
        LinearLayout selector = new LinearLayout(this);
        selector.setGravity(Gravity.CENTER);
        selector.setPadding(dp(4), dp(3), dp(4), dp(3));
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setBackground(background(COLOR_CARD, Color.TRANSPARENT, dp(24), 0));

        waterButton = button("AGUA", COLOR_CARD, COLOR_WHITE, 9);
        pillsButton = button("PASTILLAS", COLOR_CARD, COLOR_WHITE, 9);
        LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(0, dp(32), 1);
        selector.addView(waterButton, optionParams);
        selector.addView(pillsButton, new LinearLayout.LayoutParams(0, dp(32), 1));
        parent.addView(selector, new LinearLayout.LayoutParams(-1, dp(38)));

        waterButton.setOnClickListener(view -> changeKind(ReminderScheduler.KIND_WATER));
        pillsButton.setOnClickListener(view -> changeKind(ReminderScheduler.KIND_PILLS));
    }

    private void addIntervalControl(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);

        Button minusButton = button("−", COLOR_CARD, COLOR_WHITE, 19);
        intervalView = text("", 14, COLOR_WHITE, true);
        intervalView.setGravity(Gravity.CENTER);
        intervalView.setContentDescription("Intervalo del recordatorio");
        Button plusButton = button("+", COLOR_CARD, COLOR_WHITE, 19);

        row.addView(minusButton, new LinearLayout.LayoutParams(dp(34), dp(32)));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(dp(88), dp(34));
        row.addView(intervalView, labelParams);
        row.addView(plusButton, new LinearLayout.LayoutParams(dp(34), dp(32)));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-2, dp(34));
        rowParams.topMargin = dp(0);
        parent.addView(row, rowParams);

        minusButton.setOnClickListener(view -> changeInterval(-1));
        plusButton.setOnClickListener(view -> changeInterval(1));
    }

    private void changeKind(String newKind) {
        kind = newKind;
        getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE)
                .edit()
                .putString(ReminderScheduler.KEY_KIND, kind)
                .apply();
        if (isActive) {
            ReminderScheduler.scheduleNext(this);
        }
        updateScreen();
    }

    private void changeInterval(int change) {
        intervalHours = Math.max(
                ReminderScheduler.MIN_INTERVAL,
                Math.min(ReminderScheduler.MAX_INTERVAL, intervalHours + change)
        );
        getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE)
                .edit()
                .putInt(ReminderScheduler.KEY_INTERVAL, intervalHours)
                .apply();
        if (isActive) {
            ReminderScheduler.scheduleNext(this);
        }
        updateScreen();
    }

    private void toggleReminders() {
        isActive = !isActive;
        getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(ReminderScheduler.KEY_ACTIVE, isActive)
                .apply();
        if (isActive) {
            ReminderScheduler.scheduleNext(this);
        } else {
            ReminderScheduler.cancel(this);
        }
        updateScreen();
    }

    private void testVibration() {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }

        long[] pattern = {0, 180, 100, 300};
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        ReminderScheduler.showTestNotification(this);
    }

    private void requestPermissionThen(Runnable action) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingPermissionAction = action;
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST
            );
            return;
        }
        action.run();
    }

    private void updateScreen() {
        boolean water = ReminderScheduler.KIND_WATER.equals(kind);
        waterButton.setBackground(background(
                water ? COLOR_ACCENT : COLOR_CARD,
                Color.TRANSPARENT,
                dp(20),
                0
        ));
        waterButton.setTextColor(water ? COLOR_BACKGROUND : COLOR_MUTED);
        pillsButton.setBackground(background(
                water ? COLOR_CARD : COLOR_ACCENT,
                Color.TRANSPARENT,
                dp(20),
                0
        ));
        pillsButton.setTextColor(water ? COLOR_MUTED : COLOR_BACKGROUND);

        if (water) {
            reminderTitleView.setText(R.string.reminder_water_title);
            descriptionView.setText(R.string.reminder_water_description);
        } else {
            reminderTitleView.setText(R.string.reminder_pills_title);
            descriptionView.setText(R.string.reminder_pills_description);
        }
        intervalView.setText(getResources().getQuantityString(
                R.plurals.interval_hours,
                intervalHours,
                intervalHours
        ));
        startButton.setText(isActive ? R.string.pause_reminders : R.string.activate_reminders);
        statusView.setText(isActive ? R.string.reminders_active : R.string.reminders_paused);
        countdownView.setText(getCountdown());
    }

    private String getCountdown() {
        if (!isActive) {
            return formatTime(intervalHours * 60L);
        }

        long nextAlarm = getSharedPreferences(ReminderScheduler.PREFS, MODE_PRIVATE)
                .getLong(ReminderScheduler.KEY_NEXT_ALARM, 0);
        if (nextAlarm == 0) {
            return formatTime(intervalHours * 60L);
        }
        long remainingMinutes = Math.max(
                0,
                (nextAlarm - System.currentTimeMillis() + 59_999) / 60_000
        );
        return formatTime(remainingMinutes);
    }

    private static String formatTime(long minutes) {
        return String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60);
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        if (bold) {
            view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return view;
    }

    private Button button(String value, int backgroundColor, int foregroundColor, int sizeSp) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(sizeSp);
        button.setTextColor(foregroundColor);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
        button.setStateListAnimator(null);
        button.setPadding(dp(7), 0, dp(7), 0);
        button.setBackground(background(backgroundColor, Color.TRANSPARENT, dp(25), 0));
        return button;
    }

    private GradientDrawable background(int fillColor, int strokeColor, int radius, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams wrapCentered() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private LinearLayout.LayoutParams buttonLayoutParams(int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(height));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
