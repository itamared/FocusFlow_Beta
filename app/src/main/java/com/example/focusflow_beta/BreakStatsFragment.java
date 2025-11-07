package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Random;

public class BreakStatsFragment extends Fragment {

    private BarChart barChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // הטעינת Layout
        View view = inflater.inflate(R.layout.fragment_break_stats, container, false);

        barChart = view.findViewById(R.id.barChart);

        loadRandomChartData();

        return view;
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
