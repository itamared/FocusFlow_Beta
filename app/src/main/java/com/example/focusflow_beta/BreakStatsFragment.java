package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BreakStatsFragment extends Fragment {

    private BarChart barChart;
    private TextView tvWeekRange;
    private Button btnPrevWeek, btnNextWeek;

    // 0 = השבוע הנוכחי, 1 = שבוע שעבר, 2 = לפני שבועיים...
    private int weekOffset = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_break_stats, container, false);

        barChart = view.findViewById(R.id.barChart);
        tvWeekRange = view.findViewById(R.id.tvWeekRange);
        btnPrevWeek = view.findViewById(R.id.btnPrevWeek);
        btnNextWeek = view.findViewById(R.id.btnNextWeek);

        setupChart();

        btnPrevWeek.setOnClickListener(v -> {
            weekOffset += 1;
            loadWeek();
        });

        btnNextWeek.setOnClickListener(v -> {
            if (weekOffset > 0) {
                weekOffset -= 1;
                loadWeek();
            }
        });

        loadWeek();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // אם אתה בשבוע הנוכחי (offset=0) – יזוז אוטומטית כשמגיע יום ראשון וכו'
        if (weekOffset == 0) loadWeek();
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);

        // פחות שוליים כדי שהגרף "יתפרס"
        barChart.setExtraOffsets(6f, 6f, 6f, 2f);

        // X Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);          // טקסט גדול יותר
        xAxis.setYOffset(6f);            // קצת רווח מתחת לציר
        xAxis.setLabelRotationAngle(0f);
        xAxis.setLabelCount(7, true);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(6.5f);


        // Y Axis
        YAxis left = barChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setGranularity(1f);
        left.setTextSize(12f);

        barChart.getAxisRight().setEnabled(false);

        // Legend (בוצעו / פספוס / מתוכנן)
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(14f);                 // ✅ מגדיל טקסט
        legend.setFormSize(14f);                 // ✅ מגדיל ריבוע צבע
        legend.setXEntrySpace(18f);              // מרווח בין הפריטים
        legend.setYEntrySpace(10f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // לא להעמיס מספרים על כל חלק של סטאק (אם אתה רוצה להשאיר – תגיד)
        barChart.setDrawValueAboveBar(false);

        barChart.animateY(650);
    }


    private void loadWeek() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        // שבוע בישראל מתחיל ביום ראשון
        Calendar start = getWeekStartSunday(weekOffset);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);

        String startKey = dayKey(start);
        String endKey = dayKey(end);

        // כותרת טווח שבוע
        tvWeekRange.setText(prettyLabelFromKey(startKey) + " - " + prettyLabelFromKey(endKey));

        // לא מאפשרים ניווט לעתיד מעבר לשבוע הנוכחי
        btnNextWeek.setEnabled(weekOffset > 0);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("dailyStats")
                .orderBy(com.google.firebase.firestore.FieldPath.documentId(), Query.Direction.ASCENDING)
                .startAt(startKey)
                .endAt(endKey)
                .get()
                .addOnSuccessListener(qs -> {
                    Map<String, StatsDay> dayMap = new HashMap<>();
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        StatsDay d = new StatsDay(
                                safeInt(doc.getLong("plannedBreaks")),
                                safeInt(doc.getLong("startedBreaks")),
                                safeInt(doc.getLong("missedBreaks"))
                        );
                        dayMap.put(doc.getId(), d);
                    }

                    ArrayList<BarEntry> entries = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();

                    Calendar cur = (Calendar) start.clone();
                    for (int i = 0; i < 7; i++) {
                        String key = dayKey(cur);

                        // label מסודר: יום + תאריך
                        labels.add(dayNameShort(cur) + "\n" + prettyLabelFromKey(key));

                        StatsDay s = dayMap.get(key);
                        int planned = (s != null) ? s.planned : 0;
                        int started = (s != null) ? s.started : 0;
                        int missed = (s != null) ? s.missed : 0;
                        int remaining = Math.max(0, planned - started - missed);

                        // ✅ X תמיד i (0..6)
                        entries.add(new BarEntry(i, new float[]{started, missed, remaining}));

                        cur.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "שבוע");
                    dataSet.setStackLabels(new String[]{"בוצעו", "פספוס", "מתוכנן"});
                    dataSet.setColors(new int[]{
                            0xFF2E7D32, // בוצעו
                            0xFFC62828, // פספוס
                            0xFF1565C0  // מתוכנן
                    });
                    dataSet.setValueTextSize(10f);

                    BarData data = new BarData(dataSet);
                    data.setBarWidth(0.62f);
                    barChart.setData(data);

                    // labels לציר X
                    barChart.getXAxis().setLabelCount(7, true);
                    barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int idx = Math.round(value); // ✅ זה התיקון הכי חשוב
                            if (idx < 0 || idx >= labels.size()) return "";
                            return labels.get(idx);
                        }
                    });


                    barChart.invalidate();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "שגיאה בטעינת סטטיסטיקה: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    // שבוע שמתחיל ביום ראשון לפי ישראל
    private Calendar getWeekStartSunday(int weekOffset) {
        Calendar cal = Calendar.getInstance();

        // קובע ש"שבוע" מתחיל ביום ראשון
        cal.setFirstDayOfWeek(Calendar.SUNDAY);

        // זזים שבועות אחורה לפי offset
        cal.add(Calendar.WEEK_OF_YEAR, -weekOffset);

        // מקפיץ ליום ראשון של אותו שבוע
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        // מאפס שעה
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }


    private int safeInt(Long v) {
        if (v == null) return 0;
        if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return v.intValue();
    }

    // yyyyMMdd
    private String dayKey(Calendar cal) {
        return String.format(Locale.getDefault(), "%04d%02d%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    private String prettyLabelFromKey(String key) {
        // yyyyMMdd -> dd/MM
        if (key == null || key.length() != 8) return key;
        String dd = key.substring(6, 8);
        String mm = key.substring(4, 6);
        return dd + "/" + mm;
    }

    private String dayNameShort(Calendar cal) {
        // 1=Sunday ... 7=Saturday
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        switch (dow) {
            case Calendar.SUNDAY: return "א׳";
            case Calendar.MONDAY: return "ב׳";
            case Calendar.TUESDAY: return "ג׳";
            case Calendar.WEDNESDAY: return "ד׳";
            case Calendar.THURSDAY: return "ה׳";
            case Calendar.FRIDAY: return "ו׳";
            case Calendar.SATURDAY: return "ש׳";
        }
        return "";
    }

    private static class StatsDay {
        final int planned;
        final int started;
        final int missed;

        StatsDay(int planned, int started, int missed) {
            this.planned = planned;
            this.started = started;
            this.missed = missed;
        }
    }
}
