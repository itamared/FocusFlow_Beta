package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.TimePickerDialog;
import android.widget.TimePicker;
import android.widget.Toast;
import java.util.Calendar;


import androidx.fragment.app.Fragment;

import java.util.Calendar;

public class BreakTimeFragment extends Fragment {

    private LinearLayout breaksContainer;
    private Button btnNext;
    private int workStartTotal, workEndTotal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_break_time, container, false);
        breaksContainer = view.findViewById(R.id.breaksContainer);
        btnNext = view.findViewById(R.id.btnNextBreakTime);

        UserSetupData.breakTimes.clear();

        workStartTotal = getMinutes(UserSetupData.startTime);
        workEndTotal = getMinutes(UserSetupData.endTime);

        // יצירת פריטי הפסקה
        for (int i = 0; i < UserSetupData.breakCount; i++) {
            final int breakIndex = i;

            View breakItem = inflater.inflate(R.layout.item_break_time, breaksContainer, false);
            TextView tvLabel = breakItem.findViewById(R.id.tvBreakLabel);
            tvLabel.setText("הפסקה " + (breakIndex + 1));

            TextView tvStart = breakItem.findViewById(R.id.tvBreakStart);
            TextView tvEnd = breakItem.findViewById(R.id.tvBreakEnd);

            // ברירת מחדל – התחלה: workStart, סיום: +30 דקות
            int defaultEnd = Math.min(workStartTotal + 30, workEndTotal);
            tvStart.setText(formatTime(workStartTotal));
            tvEnd.setText(formatTime(defaultEnd));

            tvStart.setOnClickListener(v -> showTimePicker(tvStart));
            tvEnd.setOnClickListener(v -> showTimePicker(tvEnd));

            breaksContainer.addView(breakItem);
        }

        btnNext.setOnClickListener(v -> {
            if (validateBreakTimes()) {
                saveBreakTimesAndProceed();
            }
        });

        return view;
    }

    private void showTimePicker(TextView tv) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(getContext(), (TimePicker view, int selectedHour, int selectedMinute) -> {
            tv.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
        }, hour, minute, true).show();
    }

    private boolean validateBreakTimes() {
        for (int i = 0; i < breaksContainer.getChildCount(); i++) {
            View breakItem = breaksContainer.getChildAt(i);
            TextView tvStart = breakItem.findViewById(R.id.tvBreakStart);
            TextView tvEnd = breakItem.findViewById(R.id.tvBreakEnd);

            int start = getMinutes(tvStart.getText().toString());
            int end = getMinutes(tvEnd.getText().toString());

            if (start < workStartTotal || end > workEndTotal) {
                Toast.makeText(getContext(), "כל ההפסקות חייבות להיות בתוך שעות העבודה", Toast.LENGTH_LONG).show();
                return false;
            }

            if (start >= end) {
                Toast.makeText(getContext(), "ההתחלה חייבת להיות לפני הסיום בכל הפסקה", Toast.LENGTH_LONG).show();
                return false;
            }

            // בדיקה לחפיפות בין הפסקות
            for (int j = 0; j < breaksContainer.getChildCount(); j++) {
                if (i == j) continue;
                View otherItem = breaksContainer.getChildAt(j);
                int otherStart = getMinutes(((TextView) otherItem.findViewById(R.id.tvBreakStart)).getText().toString());
                int otherEnd = getMinutes(((TextView) otherItem.findViewById(R.id.tvBreakEnd)).getText().toString());

                if (start < otherEnd && end > otherStart) {
                    Toast.makeText(getContext(), "הפסקות חופפות! בדוק את הזמנים", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        return true;
    }

    private int getMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private String formatTime(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    private void saveBreakTimesAndProceed() {
        UserSetupData.breakTimes.clear();
        for (int i = 0; i < breaksContainer.getChildCount(); i++) {
            View breakItem = breaksContainer.getChildAt(i);
            TextView tvStart = breakItem.findViewById(R.id.tvBreakStart);
            TextView tvEnd = breakItem.findViewById(R.id.tvBreakEnd);

            UserSetupData.BreakTime b = new UserSetupData.BreakTime();
            b.start = tvStart.getText().toString();
            b.end = tvEnd.getText().toString();
            UserSetupData.breakTimes.add(b);
        }

        ((SetupActivity) requireActivity()).goToNextFragment(new SetupConfirmFragment());
    }
}
