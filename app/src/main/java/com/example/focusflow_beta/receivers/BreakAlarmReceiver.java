package com.example.focusflow_beta.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.focusflow_beta.MainActivity;
import com.example.focusflow_beta.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BreakAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "break_channel";
    private static final String PREF = "FocusFlowPrefs";

    public static final String EXTRA_BREAK_INDEX = "breakIndex";
    public static final String EXTRA_ALARM_TYPE = "alarmType";

    public static final String TYPE_START = "START";
    public static final String TYPE_END = "END";

    @Override
    public void onReceive(Context context, Intent intent) {

        final PendingResult pr = goAsync(); // מאפשר Firestore אסינכרוני

        createChannel(context);

        final int breakIndex = intent.getIntExtra(EXTRA_BREAK_INDEX, -1);
        final String alarmType = (intent.getStringExtra(EXTRA_ALARM_TYPE) != null)
                ? intent.getStringExtra(EXTRA_ALARM_TYPE)
                : TYPE_START;

        final String title = (breakIndex >= 0) ? ("הפסקה " + (breakIndex + 1)) : "הפסקה";

        // פתיחת האפליקציה בלחיצה על ההתראה
        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        final PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                3001 + Math.max(0, breakIndex),
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // כפתור "התחל טיימר" רק ל-START
        final PendingIntent confirmPending;
        if (TYPE_START.equals(alarmType)) {
            Intent confirmIntent = new Intent(context, BreakActionReceiver.class);
            confirmIntent.putExtra(EXTRA_BREAK_INDEX, breakIndex);

            confirmPending = PendingIntent.getBroadcast(
                    context,
                    3100 + Math.max(0, breakIndex),
                    confirmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } else {
            confirmPending = null;
        }

        // ב-END: אם לא התחיל את ההפסקה → נספור פספוס
        if (TYPE_END.equals(alarmType)) {
            maybeCountMissed(context, breakIndex);
        }

        // משיכה של occupation כדי לכתוב לעבוד/ללמוד (לא חובה בשביל ספירה)
        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            // fallback בלי occupation
            showNotification(context, title, alarmType, "לעבוד/ללמוד", contentIntent, confirmPending, breakIndex);
            pr.finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String occupation = doc.getString("occupation");
                    String actionWord = resolveActionWord(occupation);

                    showNotification(context, title, alarmType, actionWord, contentIntent, confirmPending, breakIndex);
                    pr.finish();
                })
                .addOnFailureListener(e -> {
                    showNotification(context, title, alarmType, "לעבוד/ללמוד", contentIntent, confirmPending, breakIndex);
                    pr.finish();
                });
    }

    /**
     * סופר פספוס רק אם המשתמש לא התחיל את ההפסקה (לפי SharedPrefs).
     * NOTE: shared pref key נכתב ב-BreakActionReceiver כשאתה לוחץ "התחל".
     */
    private void maybeCountMissed(Context context, int breakIndex) {
        if (breakIndex < 0) return;

        String dayKey = todayKey();

        boolean started = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean("started_" + dayKey + "_" + breakIndex, false);

        if (started) return;

        String uid = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) return;

        // נוודא שיש מסמך dailyStats גם אם HomeFragment לא יצר
        Map<String, Object> base = new HashMap<>();
        base.put("plannedBreaks", 0);
        base.put("startedBreaks", 0);
        base.put("missedBreaks", 0);
        base.put("lateTotalSec", 0L);
        base.put("lateCount", 0L);
        base.put("updatedAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("dailyStats")
                .document(dayKey)
                .set(base, SetOptions.merge())
                .addOnSuccessListener(v -> FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("dailyStats")
                        .document(dayKey)
                        .update(
                                "missedBreaks", FieldValue.increment(1),
                                "updatedAt", FieldValue.serverTimestamp()
                        ))
                .addOnFailureListener(e -> {
                    // אם נכשל – אין מה לעשות כאן (אפשר להוסיף Log אם תרצה)
                });
    }

    private void showNotification(Context context,
                                  String title,
                                  String alarmType,
                                  String actionWord,
                                  PendingIntent contentIntent,
                                  PendingIntent confirmPending,
                                  int breakIndex) {

        String text;
        if (TYPE_END.equals(alarmType)) {
            text = "נגמרה ההפסקה. הגיע הזמן לחזור " + actionWord + ".";
        } else {
            text = "הגיע זמן ההפסקה. כנס לאפליקציה כדי להתחיל את הטיימר.";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent);

        if (confirmPending != null && TYPE_START.equals(alarmType)) {
            builder.addAction(0, "התחל טיימר", confirmPending);
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int notifId = (TYPE_END.equals(alarmType) ? 5000 : 4000) + Math.max(0, breakIndex);
            nm.notify(notifId, builder.build());
        }
    }

    private String resolveActionWord(String occupation) {
        if (occupation == null) return "לעבוד/ללמוד";

        String o = occupation.trim().toLowerCase();

        // לימודים
        if (o.contains("לימוד") || o.contains("ללמוד") || o.contains("בית ספר")
                || o.contains("ביה\"ס") || o.contains("אוניברסיטה") || o.contains("סטודנט")
                || o.contains("תלמיד") || o.contains("כיתה") || o.contains("school") || o.contains("study")) {
            return "ללמוד";
        }

        return "לעבוד";
    }

    private String todayKey() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d%02d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Break Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
