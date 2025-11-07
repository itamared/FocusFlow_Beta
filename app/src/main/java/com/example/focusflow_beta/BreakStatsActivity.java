package com.example.focusflow_beta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Random;

public class BreakStatsActivity extends AppCompatActivity {

    private BarChart barChart;
    private Button btnBackBreakStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_break_stats);

        // הגדרת Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // ניתן להוסיף אייקון מותאם או להשאיר ברירת מחדל
            // getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }

        barChart = findViewById(R.id.barChart);
        btnBackBreakStats = findViewById(R.id.btnBackBreakStats);

        // לחיצה על הכפתור בתחתית מחזירה ל-HomeActivity
        btnBackBreakStats.setOnClickListener(v -> {
            Intent intent = new Intent(BreakStatsActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        loadRandomChartData();
    }

    // טיפול בלחיצה על חץ החזרה ב-Toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(BreakStatsActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadRandomChartData() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        Random random = new Random();

        // 7 ימים לדוגמה
        for (int i = 0; i < 7; i++) {
            float studyHours = 5 + random.nextFloat() * 3; // בין 5 ל-8 שעות
            float breakHours = 0.5f + random.nextFloat() * 1.5f; // בין 0.5 ל-2 שעות
            entries.add(new BarEntry(i, new float[]{studyHours, breakHours}));
        }

        BarDataSet dataSet = new BarDataSet(entries, "למידה / הפסקה");
        dataSet.setColors(new int[]{ColorTemplate.MATERIAL_COLORS[0], ColorTemplate.MATERIAL_COLORS[1]});
        dataSet.setStackLabels(new String[]{"למידה", "הפסקה"});

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1500);
        barChart.invalidate();
    }
}
