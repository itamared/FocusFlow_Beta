package com.example.focusflow_beta;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class BreakStatsActivity extends AppCompatActivity {

    private SimpleBarGraphView barGraphView;
    private TextView tvBreakStatus;
    private Button btnBackBreakStats, btnToday, btnThreeDays, btnWeek;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_break_stats);

        barGraphView = findViewById(R.id.barGraphView);
        tvBreakStatus = findViewById(R.id.tvBreakStatus);
        btnBackBreakStats = findViewById(R.id.btnBackBreakStats);
        btnToday = findViewById(R.id.btnToday);
        btnThreeDays = findViewById(R.id.btnThreeDays);
        btnWeek = findViewById(R.id.btnWeek);

        // כפתור חזור
        btnBackBreakStats.setOnClickListener(v -> finish());

        // ברירת מחדל - הצגת היום
        loadChart("today");

        btnToday.setOnClickListener(v -> loadChart("today"));
        btnThreeDays.setOnClickListener(v -> loadChart("3days"));
        btnWeek.setOnClickListener(v -> loadChart("week"));
    }

    private void loadChart(String mode) {
        float studyHours = 7f;    // לדוגמה
        float breakHours = 1f;    // לדוגמה

        if (mode.equals("today")) {
            studyHours = 7f;
            breakHours = 1f;
        } else if (mode.equals("3days")) {
            studyHours = 6f;
            breakHours = 1f;
        } else if (mode.equals("week")) {
            studyHours = 35f / 7; // ממוצע
            breakHours = 7f / 7;
        }

        // הצגת ברגרף
        barGraphView.setData(studyHours, breakHours);

        // חישוב סטטוס
        float targetBreak = 0.75f; // לדוגמה - 45 דקות = 0.75 שעות
        if (breakHours > targetBreak) {
            tvBreakStatus.setText("בחריגה: לקחת " + (int)((breakHours - targetBreak)*60) + " דקות יותר מהיעד שלך.");
            tvBreakStatus.setTextColor(0xFFD32F2F); // אדום
        } else if (breakHours < targetBreak) {
            tvBreakStatus.setText("לא ניצלת את כל ההפסקה שלך – נשארו " + (int)((targetBreak - breakHours)*60) + " דקות.");
            tvBreakStatus.setTextColor(0xFF1976D2); // כחול
        } else {
            tvBreakStatus.setText("בול על היעד! כל הכבוד 🎯");
            tvBreakStatus.setTextColor(0xFF388E3C); // ירוק
        }
    }
}
