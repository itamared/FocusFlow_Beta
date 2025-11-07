package com.example.focusflow_beta.oldactivities;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.widget.Toolbar;

import com.example.focusflow_beta.BaseActivity;
import com.example.focusflow_beta.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Random;

public class BreakStatsActivity extends BaseActivity {

    private BarChart barChart;
    private Button btnBackBreakStats;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_break_stats_old);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        barChart = findViewById(R.id.barChart);
        btnBackBreakStats = findViewById(R.id.btnBackBreakStats);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        loadRandomChartData();

        // כפתור חזור
        btnBackBreakStats.setOnClickListener(v -> finish());

        // NavBar אחיד
        setupBottomNavigation(bottomNavigationView, R.id.nav_stats);
    }

    private void loadRandomChartData() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 7; i++) {
            float studyHours = 5 + random.nextFloat() * 3;
            float breakHours = 0.5f + random.nextFloat() * 1.5f;
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
