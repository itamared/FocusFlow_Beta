package com.example.focusflow_beta;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.focusflow_beta.receivers.BreakAlarmReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvStatus, tvTimer, tvWindow, tvSpare, tvNextBreak;
    private Button btnStart, btnAddSpare, btnReset;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable ticker;

    // break state
    private int currentBreakIndex = -1;
    private long scheduledStartMillis = 0L;
    private long scheduledEndMillis = 0L;

    private boolean isRunning = false;
    private long actualStartMillis = 0L;

    // spare from late start (ms)
    private long spareMillis = 0L;

    // spare saved to extend the NEXT break (ms)
    private long carryMillis = 0L;

    private boolean didLoadOnce = false; // ✅ מונע טעינת Firestore כל מעבר טאבים

    private static final String PREF = "FocusFlowPrefs";
    private static final String K_RUNNING = "running";
    private static final String K_ACTUAL_START = "actualStartMillis";
    private static final String K_CUR_INDEX = "currentBreakIndex";
    private static final String K_SCHEDULED_START = "scheduledStartMillis";
    private static final String K_SCHEDULED_END = "scheduledEndMillis";
    private static final String K_SPARE = "spareMillis";
    private static final String K_CARRY = "carryMillis";
    private static final String K_ALARMS_DAY = "alarmsScheduledDayKey"; // yyyyMMdd

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvStatus = view.findViewById(R.id.tvStatus);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvWindow = view.findViewById(R.id.tvWindow);
        tvSpare = view.findViewById(R.id.tvSpare);
        tvNextBreak = view.findViewById(R.id.tvNextBreak);

        btnStart = view.findViewById(R.id.btnStart);
        btnAddSpare = view.findViewById(R.id.btnAddSpare);
        btnReset = view.findViewById(R.id.btnReset);

        btnStart.setOnClickListener(v -> startBreakTimer());
        btnAddSpare.setOnClickListener(v -> addSpareToNextBreak());
        btnReset.setOnClickListener(v -> resetAllLocalForTesting());

        loadState();

        // ✅ אל תטען כל פעם שחוזרים לטאב. נטען רק פעם אחת (או אם אין נתונים)
        if (!didLoadOnce || UserSetupData.breakTimes == null || UserSetupData.breakTimes.isEmpty()) {
            loadBreaksFromFirestoreAndInit();
            didLoadOnce = true;
        } else {
            // אם כבר יש נתונים, רק ניישר UI
            updateUIOnce();
        }

        startTicker();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // אם המשתמש הגיע מההתראה ולחץ "התחל טיימר"
        Context c = getContext();
        if (c == null) return;

        boolean startBreakNow = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean("startBreakNow", false);

        int breakIndexFromNotif = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt("breakIndexToStart", -1);

        if (startBreakNow) {
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                    .putBoolean("startBreakNow", false)
                    .putInt("breakIndexToStart", -1)
                    .apply();

            // לבחור הפסקה לפי אינדקס מההתראה
            if (breakIndexFromNotif >= 0) {
                selectBreakByIndex(breakIndexFromNotif, /*resetTimer*/ false);
            } else {
                // רק אם אין חלון בכלל
                if (scheduledStartMillis == 0L || scheduledEndMillis == 0L) {
                    pickActiveOrNextBreak(false);
                }
            }
        }

        // מסמן פספוסים אם עבר זמן
        reconcileMissedBreaksForToday();
        updateUIOnce();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopTicker();
    }

    // =========================================================
    // Core behavior
    // =========================================================

    private void startBreakTimer() {
        if (getContext() == null) return;

        // אם אין בחירה – נבחר הפסקה פעילה/באה
        if (currentBreakIndex == -1 || scheduledEndMillis == 0L) {
            pickActiveOrNextBreak(false);
        }

        if (scheduledStartMillis == 0L || scheduledEndMillis == 0L) {
            tvStatus.setText("אין הפסקה מוגדרת להיום");
            return;
        }

        long now = System.currentTimeMillis();

        // ✅ חסימה לפני תחילת החלון
        if (now < scheduledStartMillis) {
            long wait = scheduledStartMillis - now;
            tvStatus.setText("עוד לא הגיע הזמן. מתחיל בעוד " + fmtDuration(wait));
            Toast.makeText(getContext(), "אפשר להתחיל רק כשהשעה מגיעה ✅", Toast.LENGTH_SHORT).show();
            updateUIOnce();
            return;
        }

        // אם כבר נגמרה
        if (now >= scheduledEndMillis) {
            tvStatus.setText("ההפסקה כבר הסתיימה");
            isRunning = false;
            actualStartMillis = 0L;
            saveState();
            updateUIOnce();
            return;
        }

        // התחלה רק פעם אחת
        if (!isRunning) {
            isRunning = true;
            actualStartMillis = now;

            // ספייר = איחור מתחילת ההפסקה (אם התחלת ב-16:02 במקום 16:00 -> 2 דקות ספייר)
            spareMillis = Math.max(0L, actualStartMillis - scheduledStartMillis);

            // ✅ סטטיסטיקה: started + late
            markBreakStartedStats(currentBreakIndex, scheduledStartMillis, actualStartMillis);

            tvStatus.setText("טיימר הפסקה התחיל ☕");
            saveState();
        }

        updateUIOnce();
    }

    private void addSpareToNextBreak() {
        if (getContext() == null) return;

        if (spareMillis <= 0) {
            Toast.makeText(getContext(), "אין ספייר להעברה", Toast.LENGTH_SHORT).show();
            return;
        }

        carryMillis += spareMillis;
        spareMillis = 0L;

        Toast.makeText(getContext(), "הספייר נוסף להפסקה הבאה ✅", Toast.LENGTH_SHORT).show();
        saveState();
        updateUIOnce();
    }

    private void resetAllLocalForTesting() {
        Context c = getContext();
        if (c == null) return;

        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .remove(K_RUNNING)
                .remove(K_ACTUAL_START)
                .remove(K_CUR_INDEX)
                .remove(K_SCHEDULED_START)
                .remove(K_SCHEDULED_END)
                .remove(K_SPARE)
                .remove(K_CARRY)
                .apply();

        currentBreakIndex = -1;
        scheduledStartMillis = 0L;
        scheduledEndMillis = 0L;
        isRunning = false;
        actualStartMillis = 0L;
        spareMillis = 0L;
        carryMillis = 0L;

        tvStatus.setText("התאפס ✅");
        updateUIOnce();
    }

    // =========================================================
    // UI ticker
    // =========================================================

    private void startTicker() {
        if (ticker != null) return;
        ticker = new Runnable() {
            @Override
            public void run() {
                updateUIOnce();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(ticker);
    }

    private void stopTicker() {
        if (ticker != null) {
            handler.removeCallbacks(ticker);
            ticker = null;
        }
    }

    private void updateUIOnce() {
        long now = System.currentTimeMillis();

        // חלון נוכחי
        if (scheduledStartMillis > 0 && scheduledEndMillis > 0) {
            tvWindow.setText("הפסקה: " + fmtTime(scheduledStartMillis) + " - " + fmtTime(scheduledEndMillis));
        } else {
            tvWindow.setText("הפסקה: --:-- - --:--");
        }

        // לפני הזמן: כפתור התחלה כבוי + טיימר "מתחיל בעוד"
        if (!isRunning && scheduledStartMillis > 0 && now < scheduledStartMillis) {
            long wait = scheduledStartMillis - now;
            tvTimer.setText(fmtDuration(wait));
            tvStatus.setText("ממתין להתחלת ההפסקה ⏳");
            btnStart.setEnabled(false);
        } else {
            btnStart.setEnabled(true);

            // טיימר מוצג
            if (!isRunning || scheduledEndMillis == 0L) {
                tvTimer.setText("--:--");
            } else {
                long remaining = Math.max(0L, scheduledEndMillis - now);
                tvTimer.setText(fmtDuration(remaining));
            }
        }

        // ספייר + כפתור
        tvSpare.setText("ספייר נוכחי: " + fmtDuration(spareMillis));
        btnAddSpare.setEnabled(spareMillis > 0);

        // הפסקה הבאה (אם יש carry מציג הארכה)
        showNextBreakInfo();

        // אם נגמרה
        if (isRunning && scheduledEndMillis > 0L && now >= scheduledEndMillis) {
            isRunning = false;
            actualStartMillis = 0L;
            tvStatus.setText("ההפסקה הסתיימה ✅");
            saveState();
        }
    }

    private void showNextBreakInfo() {
        if (UserSetupData.breakTimes == null || UserSetupData.breakTimes.isEmpty()) {
            tvNextBreak.setText("הפסקה הבאה: -");
            return;
        }

        int nextIndex = (currentBreakIndex >= 0) ? currentBreakIndex + 1 : findActiveOrNextIndex();
        if (nextIndex < 0 || nextIndex >= UserSetupData.breakTimes.size()) {
            tvNextBreak.setText("הפסקה הבאה: אין עוד הפסקות להיום");
            return;
        }

        UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(nextIndex);
        long ns = parseTodayMillis(bt.start);
        long ne = parseTodayMillis(bt.end);

        if (ns == 0 || ne == 0) {
            tvNextBreak.setText("הפסקה הבאה: -");
            return;
        }

        if (carryMillis > 0) {
            tvNextBreak.setText("הפסקה הבאה: " + fmtTime(ns) + " - " + fmtTime(ne) +
                    "  → עם ספייר: עד " + fmtTime(ne + carryMillis));
        } else {
            tvNextBreak.setText("הפסקה הבאה: " + fmtTime(ns) + " - " + fmtTime(ne));
        }
    }

    // =========================================================
    // Firestore load + schedule alarms (START + END)
    // =========================================================

    private void loadBreaksFromFirestoreAndInit() {
        Context context = getContext();
        if (context == null) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            tvStatus.setText("לא מחובר");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tvStatus.setText("טוען נתונים…");

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    Object raw = doc.get("breakTimes");
                    if (!(raw instanceof List)) {
                        tvStatus.setText("אין הפסקות מוגדרות");
                        return;
                    }

                    List<?> list = (List<?>) raw;
                    List<UserSetupData.BreakTime> parsed = new ArrayList<>();

                    for (Object item : list) {
                        if (item instanceof Map) {
                            Map<?, ?> m = (Map<?, ?>) item;
                            Object s = m.get("start");
                            Object e = m.get("end");
                            if (s != null && e != null) {
                                parsed.add(new UserSetupData.BreakTime(String.valueOf(s), String.valueOf(e)));
                            }
                        }
                    }

                    UserSetupData.breakTimes = parsed;

                    // ✅ dailyStats: לא לדרוס! רק ליצור אם לא קיים, ולעדכן plannedBreaks אם צריך
                    ensureDailyStatsDocSafe(parsed.size());

                    // תזמון התראות (רק פעם ביום)
                    scheduleAlarmsOncePerDay();

                    // ✅ אל תאפס טיימר: רק אם אין חלון בכלל או שאין ריצה
                    if (!isRunning && (scheduledStartMillis == 0L || scheduledEndMillis == 0L)) {
                        pickActiveOrNextBreak(true);
                    }

                    tvStatus.setText("מוכן ✅");
                    updateUIOnce();
                })
                .addOnFailureListener(e -> tvStatus.setText("שגיאה: " + e.getMessage()));
    }

    private void scheduleAlarmsOncePerDay() {
        Context context = getContext();
        if (context == null) return;

        String todayKey = dayKey(System.currentTimeMillis());
        String savedKey = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(K_ALARMS_DAY, "");

        if (todayKey.equals(savedKey)) return;

        scheduleAllBreakAlarmsStartAndEnd();

        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(K_ALARMS_DAY, todayKey)
                .apply();
    }

    private void scheduleAllBreakAlarmsStartAndEnd() {
        Context context = getContext();
        if (context == null) return;
        if (UserSetupData.breakTimes == null || UserSetupData.breakTimes.isEmpty()) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long now = System.currentTimeMillis();

        for (int i = 0; i < UserSetupData.breakTimes.size(); i++) {
            UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(i);

            long startAt = parseTodayMillis(bt.start);
            long endAt = parseTodayMillis(bt.end);

            if (startAt <= 0 || endAt <= 0) continue;
            if (endAt <= startAt) continue;

            // START
            cancelAlarm(context, am, 20000 + i);
            if (startAt > now) {
                Intent startIntent = new Intent(context, BreakAlarmReceiver.class);
                startIntent.putExtra(BreakAlarmReceiver.EXTRA_BREAK_INDEX, i);
                startIntent.putExtra(BreakAlarmReceiver.EXTRA_ALARM_TYPE, BreakAlarmReceiver.TYPE_START);

                PendingIntent startPI = PendingIntent.getBroadcast(
                        context,
                        20000 + i,
                        startIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startAt, startPI);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, startAt, startPI);
                    }
                } catch (SecurityException ignored) {}
            }

            // END
            cancelAlarm(context, am, 21000 + i);
            if (endAt > now) {
                Intent endIntent = new Intent(context, BreakAlarmReceiver.class);
                endIntent.putExtra(BreakAlarmReceiver.EXTRA_BREAK_INDEX, i);
                endIntent.putExtra(BreakAlarmReceiver.EXTRA_ALARM_TYPE, BreakAlarmReceiver.TYPE_END);

                PendingIntent endPI = PendingIntent.getBroadcast(
                        context,
                        21000 + i,
                        endIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, endPI);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, endAt, endPI);
                    }
                } catch (SecurityException ignored) {}
            }
        }
    }

    private void cancelAlarm(Context context, AlarmManager am, int requestCode) {
        Intent intent = new Intent(context, BreakAlarmReceiver.class);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }

    // =========================================================
    // Daily Stats (SAFE)
    // =========================================================

    private String todayKey() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d%02d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    // ✅ לא דורס started/missed! רק יוצר אם אין מסמך, או מעדכן plannedBreaks אם צריך
    private void ensureDailyStatsDocSafe(int plannedBreaks) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dayKey = todayKey();

        DocumentReference dayDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("dailyStats")
                .document(dayKey);

        dayDoc.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                Map<String, Object> base = new HashMap<>();
                base.put("plannedBreaks", plannedBreaks);
                base.put("startedBreaks", 0);
                base.put("missedBreaks", 0);
                base.put("lateTotalSec", 0L);
                base.put("lateCount", 0L);
                base.put("updatedAt", FieldValue.serverTimestamp());
                dayDoc.set(base, SetOptions.merge());
            } else {
                // רק לשמור plannedBreaks מעודכן
                dayDoc.set(new HashMap<String, Object>() {{
                    put("plannedBreaks", plannedBreaks);
                    put("updatedAt", FieldValue.serverTimestamp());
                }}, SetOptions.merge());
            }
        });
    }

    private void markBreakStartedStats(int breakIndex, long scheduledStartMs, long actualStartMs) {
        if (getContext() == null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dayKey = todayKey();

        long lateSec = Math.max(0L, (actualStartMs - scheduledStartMs) / 1000L);

        // כדי שה-END alarm / reconcile ידע שזה לא פספוס
        getContext().getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("started_" + dayKey + "_" + breakIndex, true)
                .apply();

        DocumentReference dayDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("dailyStats")
                .document(dayKey);

        Map<String, Object> patch = new HashMap<>();
        patch.put("plannedBreaks", UserSetupData.breakTimes != null ? UserSetupData.breakTimes.size() : 0);
        patch.put("startedBreaks", FieldValue.increment(1));
        patch.put("lateTotalSec", FieldValue.increment(lateSec));
        patch.put("lateCount", FieldValue.increment(1));
        patch.put("updatedAt", FieldValue.serverTimestamp());

        dayDoc.set(patch, SetOptions.merge());
    }

    private void reconcileMissedBreaksForToday() {
        Context context = getContext();
        if (context == null) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (UserSetupData.breakTimes == null || UserSetupData.breakTimes.isEmpty()) return;

        long now = System.currentTimeMillis();
        String dayKey = dayKey(now);

        DocumentReference dayDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("dailyStats")
                .document(dayKey);

        for (int i = 0; i < UserSetupData.breakTimes.size(); i++) {
            UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(i);

            long endAt = parseTodayMillis(bt.end);
            if (endAt <= 0) continue;

            if (now < endAt) continue;

            boolean started = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getBoolean("started_" + dayKey + "_" + i, false);
            if (started) continue;

            boolean alreadyMissed = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getBoolean("missed_" + dayKey + "_" + i, false);
            if (alreadyMissed) continue;

            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                    .putBoolean("missed_" + dayKey + "_" + i, true)
                    .apply();

            Map<String, Object> patch = new HashMap<>();
            patch.put("plannedBreaks", UserSetupData.breakTimes.size());
            patch.put("missedBreaks", FieldValue.increment(1));
            patch.put("updatedAt", FieldValue.serverTimestamp());

            dayDoc.set(patch, SetOptions.merge());
        }
    }

    // =========================================================
    // Break selection (בלי לאפס ריצה בטעות)
    // =========================================================

    private void selectBreakByIndex(int index, boolean resetTimer) {
        if (UserSetupData.breakTimes == null || UserSetupData.breakTimes.isEmpty()) return;
        if (index < 0 || index >= UserSetupData.breakTimes.size()) return;

        currentBreakIndex = index;

        UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(index);
        scheduledStartMillis = parseTodayMillis(bt.start);

        long baseEnd = parseTodayMillis(bt.end);

        if (carryMillis > 0 && index > 0) {
            baseEnd = baseEnd + carryMillis;
            carryMillis = 0L;
        }
        scheduledEndMillis = baseEnd;

        // ✅ אל תאפס ריצה אם לא ביקשנו
        if (resetTimer) {
            isRunning = false;
            actualStartMillis = 0L;
            spareMillis = 0L;
        }

        saveState();
    }

    private void pickActiveOrNextBreak(boolean resetTimer) {
        int idx = findActiveOrNextIndex();
        if (idx >= 0) selectBreakByIndex(idx, resetTimer);
    }

    private int findActiveOrNextIndex() {
        if (UserSetupData.breakTimes == null || UserSetupData.breakTimes.isEmpty()) return -1;

        long now = System.currentTimeMillis();

        int bestIndex = -1;
        long bestStart = Long.MAX_VALUE;

        for (int i = 0; i < UserSetupData.breakTimes.size(); i++) {
            UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(i);
            long s = parseTodayMillis(bt.start);
            long e = parseTodayMillis(bt.end);

            if (s == 0 || e == 0) continue;

            if (now >= s && now < e) return i;

            if (now < s && s < bestStart) {
                bestStart = s;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    // =========================================================
    // Helpers + persistence
    // =========================================================

    private long parseTodayMillis(String hhmm) {
        try {
            String[] parts = hhmm.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, h);
            cal.set(Calendar.MINUTE, m);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String fmtTime(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    private String fmtDuration(long millis) {
        long totalSec = millis / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    private String dayKey(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH) + 1;
        int d = cal.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.getDefault(), "%04d%02d%02d", y, m, d);
    }

    private void saveState() {
        Context c = getContext();
        if (c == null) return;

        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putBoolean(K_RUNNING, isRunning)
                .putLong(K_ACTUAL_START, actualStartMillis)
                .putInt(K_CUR_INDEX, currentBreakIndex)
                .putLong(K_SCHEDULED_START, scheduledStartMillis)
                .putLong(K_SCHEDULED_END, scheduledEndMillis)
                .putLong(K_SPARE, spareMillis)
                .putLong(K_CARRY, carryMillis)
                .apply();
    }

    private void loadState() {
        Context c = getContext();
        if (c == null) return;

        isRunning = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(K_RUNNING, false);
        actualStartMillis = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(K_ACTUAL_START, 0L);
        currentBreakIndex = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(K_CUR_INDEX, -1);
        scheduledStartMillis = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(K_SCHEDULED_START, 0L);
        scheduledEndMillis = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(K_SCHEDULED_END, 0L);
        spareMillis = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(K_SPARE, 0L);
        carryMillis = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(K_CARRY, 0L);
    }
}
