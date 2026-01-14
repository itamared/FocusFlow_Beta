package com.example.focusflow_beta;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.focusflow_beta.receivers.BreakAlarmReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private Spinner spOccupation, spBreakCount;
    private EditText etWorkStart, etWorkEnd;
    private LinearLayout breaksContainer;
    private Button btnSave;

    // כדי שנוכל לשמר ערכים כשמשנים breakCount
    private final List<String> cachedStarts = new ArrayList<>();
    private final List<String> cachedEnds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        spOccupation = view.findViewById(R.id.spOccupation);
        spBreakCount = view.findViewById(R.id.spBreakCount);
        etWorkStart = view.findViewById(R.id.etWorkStart);
        etWorkEnd = view.findViewById(R.id.etWorkEnd);
        breaksContainer = view.findViewById(R.id.breaksContainer);
        btnSave = view.findViewById(R.id.btnSaveSettings);

        setupSpinners();
        loadFromFirestore();

        spBreakCount.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                int newCount = position + 1; // 1..N
                rebuildBreakRows(newCount, true);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(v -> saveToFirestoreAndResync());

        return view;
    }

    // =========================
    // Spinners
    // =========================

    private void setupSpinners() {
        Context context = getContext();
        if (context == null) return;

        // Occupation: 2 אופציות בלבד
        ArrayAdapter<String> occAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                new String[]{"עבודה", "לימודים"}
        );
        occAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOccupation.setAdapter(occAdapter);

        // BreakCount: נניח 1..10 (אפשר לשנות)
        List<String> counts = new ArrayList<>();
        for (int i = 1; i <= 10; i++) counts.add(String.valueOf(i));

        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                counts
        );
        countAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBreakCount.setAdapter(countAdapter);
    }

    // =========================
    // Load from Firestore
    // =========================

    private void loadFromFirestore() {
        Context context = getContext();
        if (context == null) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(context, "אתה לא מחובר", Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String occupation = doc.getString("occupation");
                    String startTime = doc.getString("startTime");
                    String endTime = doc.getString("endTime");

                    etWorkStart.setText(startTime != null ? startTime : "");
                    etWorkEnd.setText(endTime != null ? endTime : "");

                    // occupation spinner select
                    if ("לימודים".equals(occupation)) spOccupation.setSelection(1);
                    else spOccupation.setSelection(0);

                    // break count
                    int breakCount = 1;
                    Long bc = doc.getLong("breakCount");
                    if (bc != null) breakCount = Math.max(1, Math.min(10, bc.intValue()));
                    spBreakCount.setSelection(breakCount - 1);

                    // breakTimes list
                    cachedStarts.clear();
                    cachedEnds.clear();

                    Object raw = doc.get("breakTimes");
                    if (raw instanceof List) {
                        List<?> list = (List<?>) raw;
                        for (Object item : list) {
                            if (item instanceof Map) {
                                Map<?, ?> m = (Map<?, ?>) item;
                                String s = m.get("start") != null ? String.valueOf(m.get("start")) : "";
                                String e = m.get("end") != null ? String.valueOf(m.get("end")) : "";
                                cachedStarts.add(s);
                                cachedEnds.add(e);
                            }
                        }
                    }

                    rebuildBreakRows(breakCount, false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "שגיאה בטעינה: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // =========================
    // Break rows UI (בדיוק N שורות)
    // =========================

    private void rebuildBreakRows(int count, boolean fromUserChange) {
        Context context = getContext();
        if (context == null) return;

        // לפני שבונים מחדש: נשמור מה שיש כרגע במסך (כדי לא לאבד)
        if (fromUserChange) cacheCurrentRows();

        breaksContainer.removeAllViews();

        // אם אין נתונים בכלל, נאפס קאש לגודל המתאים
        while (cachedStarts.size() < count) cachedStarts.add("");
        while (cachedEnds.size() < count) cachedEnds.add("");
        if (cachedStarts.size() > count) cachedStarts.subList(count, cachedStarts.size()).clear();
        if (cachedEnds.size() > count) cachedEnds.subList(count, cachedEnds.size()).clear();

        for (int i = 0; i < count; i++) {
            View row = LayoutInflater.from(context).inflate(R.layout.row_break_time, breaksContainer, false);

            EditText etS = row.findViewById(R.id.etBreakStart);
            EditText etE = row.findViewById(R.id.etBreakEnd);
            Button btnRemove = row.findViewById(R.id.btnRemoveBreak);

            etS.setText(cachedStarts.get(i));
            etE.setText(cachedEnds.get(i));

            // אין אפשרות למחוק שורה כי מספר ההפסקות נשלט ע"י Spinner
            btnRemove.setEnabled(false);
            btnRemove.setAlpha(0.35f);
            btnRemove.setOnClickListener(null);

            breaksContainer.addView(row);
        }
    }

    private void cacheCurrentRows() {
        cachedStarts.clear();
        cachedEnds.clear();

        for (int i = 0; i < breaksContainer.getChildCount(); i++) {
            View row = breaksContainer.getChildAt(i);
            EditText etS = row.findViewById(R.id.etBreakStart);
            EditText etE = row.findViewById(R.id.etBreakEnd);
            cachedStarts.add(etS.getText().toString().trim());
            cachedEnds.add(etE.getText().toString().trim());
        }
    }

    // =========================
    // Save + Validate + Sort + Resync alarms
    // =========================

    private void saveToFirestoreAndResync() {
        Context context = getContext();
        if (context == null) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(context, "אתה לא מחובר", Toast.LENGTH_LONG).show();
            return;
        }

        String occupation = String.valueOf(spOccupation.getSelectedItem());
        String workStart = etWorkStart.getText().toString().trim();
        String workEnd = etWorkEnd.getText().toString().trim();

        int breakCount = spBreakCount.getSelectedItemPosition() + 1;

        if (TextUtils.isEmpty(workStart) || TextUtils.isEmpty(workEnd)) {
            Toast.makeText(context, "מלא שעות עבודה", Toast.LENGTH_LONG).show();
            return;
        }

        Integer workStartMin = parseHHmmToMinutes(workStart);
        Integer workEndMin = parseHHmmToMinutes(workEnd);
        if (workStartMin == null || workEndMin == null) {
            Toast.makeText(context, "שעות עבודה לא בפורמט HH:mm", Toast.LENGTH_LONG).show();
            return;
        }
        if (workEndMin <= workStartMin) {
            Toast.makeText(context, "שעת סיום חייבת להיות אחרי שעת התחלה", Toast.LENGTH_LONG).show();
            return;
        }

        // אוספים את ההפסקות מה-UI (בדיוק breakCount)
        List<UserSetupData.BreakTime> breakObjects = new ArrayList<>();
        for (int i = 0; i < breaksContainer.getChildCount(); i++) {
            View row = breaksContainer.getChildAt(i);
            EditText etS = row.findViewById(R.id.etBreakStart);
            EditText etE = row.findViewById(R.id.etBreakEnd);

            String s = etS.getText().toString().trim();
            String e = etE.getText().toString().trim();

            if (TextUtils.isEmpty(s) || TextUtils.isEmpty(e)) {
                Toast.makeText(context, "יש הפסקה עם שעה חסרה", Toast.LENGTH_LONG).show();
                return;
            }

            Integer sMin = parseHHmmToMinutes(s);
            Integer eMin = parseHHmmToMinutes(e);
            if (sMin == null || eMin == null) {
                Toast.makeText(context, "שעת הפסקה לא בפורמט HH:mm", Toast.LENGTH_LONG).show();
                return;
            }
            if (eMin <= sMin) {
                Toast.makeText(context, "סיום הפסקה חייב להיות אחרי התחלה", Toast.LENGTH_LONG).show();
                return;
            }

            // חייב להיות בתוך טווח העבודה
            if (sMin < workStartMin || eMin > workEndMin) {
                Toast.makeText(context, "הפסקות חייבות להיות בתוך טווח העבודה/לימודים", Toast.LENGTH_LONG).show();
                return;
            }

            breakObjects.add(new UserSetupData.BreakTime(normalizeHHmm(s), normalizeHHmm(e)));
        }

        // ✅ מיון אוטומטי לפי התחלה (מוקדם -> מאוחר)
        Collections.sort(breakObjects, new Comparator<UserSetupData.BreakTime>() {
            @Override
            public int compare(UserSetupData.BreakTime a, UserSetupData.BreakTime b) {
                Integer am = parseHHmmToMinutes(a.start);
                Integer bm = parseHHmmToMinutes(b.start);
                if (am == null || bm == null) return 0;
                return am - bm;
            }
        });

        // בונים breakTimes ל-Firestore
        List<Map<String, String>> breakList = new ArrayList<>();
        for (UserSetupData.BreakTime bt : breakObjects) {
            Map<String, String> m = new HashMap<>();
            m.put("start", bt.start);
            m.put("end", bt.end);
            breakList.add(m);
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("occupation", occupation);
        userData.put("startTime", normalizeHHmm(workStart));
        userData.put("endTime", normalizeHHmm(workEnd));
        userData.put("breakCount", breakCount);
        userData.put("breakTimes", breakList);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(userData)
                .addOnSuccessListener(unused -> {

                    // עדכון זיכרון מקומי
                    UserSetupData.occupation = occupation;
                    UserSetupData.startTime = normalizeHHmm(workStart);
                    UserSetupData.endTime = normalizeHHmm(workEnd);
                    UserSetupData.breakCount = breakCount;
                    UserSetupData.breakTimes = breakObjects;

                    // תזמון מחדש
                    cancelAllBreakAlarms(context);
                    scheduleAllBreakAlarmsStartAndEnd(context, breakObjects);

                    // גורם ל-HomeFragment לתזמן מחדש יום חדש
                    context.getSharedPreferences("FocusFlowPrefs", Context.MODE_PRIVATE)
                            .edit()
                            .remove("alarmsScheduledDayKey")
                            .apply();

                    // עדכון UI של השורות לפי המיון (כדי שתראה שזה הסתדר)
                    cachedStarts.clear();
                    cachedEnds.clear();
                    for (UserSetupData.BreakTime bt : breakObjects) {
                        cachedStarts.add(bt.start);
                        cachedEnds.add(bt.end);
                    }
                    rebuildBreakRows(breakCount, false);

                    Toast.makeText(context, "נשמר ✅ (הפסקות סודרו אוטומטית)", Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // =========================
    // Alarm Resync (cancel + schedule START+END)
    // =========================

    private void cancelAllBreakAlarms(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // HomeFragment משתמש: 20000+i (START), 21000+i (END)
        for (int i = 0; i < 50; i++) { // טווח “מספיק גדול”
            cancelOne(am, context, 20000 + i);
            cancelOne(am, context, 21000 + i);
        }
    }

    private void cancelOne(AlarmManager am, Context context, int requestCode) {
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

    private void scheduleAllBreakAlarmsStartAndEnd(Context context, List<UserSetupData.BreakTime> breaks) {
        if (breaks == null || breaks.isEmpty()) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // Exact alarms permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(context, "כדי לקבל התראות בזמן, אשר Exact Alarms בהגדרות.", Toast.LENGTH_LONG).show();
                try {
                    Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                } catch (Exception ignored) {}
                return;
            }
        }

        long now = System.currentTimeMillis();

        for (int i = 0; i < breaks.size(); i++) {
            UserSetupData.BreakTime bt = breaks.get(i);

            long startAt = parseTodayMillis(bt.start);
            long endAt = parseTodayMillis(bt.end);

            if (startAt <= 0 || endAt <= 0 || endAt <= startAt) continue;

            // START
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

    // =========================
    // Time helpers
    // =========================

    private Integer parseHHmmToMinutes(String hhmm) {
        try {
            String[] p = hhmm.trim().split(":");
            if (p.length != 2) return null;
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return h * 60 + m;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeHHmm(String hhmm) {
        Integer min = parseHHmmToMinutes(hhmm);
        if (min == null) return hhmm;
        int h = min / 60;
        int m = min % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    private long parseTodayMillis(String hhmm) {
        Integer min = parseHHmmToMinutes(hhmm);
        if (min == null) return 0L;

        int h = min / 60;
        int m = min % 60;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
