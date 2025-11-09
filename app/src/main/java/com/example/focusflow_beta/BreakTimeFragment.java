package com.example.focusflow_beta;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;

public class BreakTimeFragment extends Fragment {

    private LinearLayout breaksContainer;
    private Button btnNext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_break_time, container, false);

        breaksContainer = view.findViewById(R.id.breaksContainer);
        btnNext = view.findViewById(R.id.btnNextBreakTime);

        // נקה רשימה קיימת
        UserSetupData.breakTimes.clear();

        // יוצרים כמה פריטים לפי מספר ההפסקות שנבחר
        for (int i = 0; i < UserSetupData.breakCount; i++) {
            View breakItem = inflater.inflate(R.layout.item_break_time, breaksContainer, false);
            TextView tvLabel = breakItem.findViewById(R.id.tvBreakLabel);
            tvLabel.setText("הפסקה " + (i + 1));

            TextView tvStart = breakItem.findViewById(R.id.tvBreakStart);
            TextView tvEnd = breakItem.findViewById(R.id.tvBreakEnd);

            // ברירת מחדל – 10:00 - 10:30 (ניתן לשנות)
            tvStart.setText("10:00");
            tvEnd.setText("10:30");

            // פותחים TimePicker לכל TextView
            tvStart.setOnClickListener(v -> showTimePicker(tvStart));
            tvEnd.setOnClickListener(v -> showTimePicker(tvEnd));

            breaksContainer.addView(breakItem);
        }

        btnNext.setOnClickListener(v -> {
            // שמירה ל-UserSetupData
            UserSetupData.breakTimes.clear();
            for (int i = 0; i < breaksContainer.getChildCount(); i++) {
                View breakItem = breaksContainer.getChildAt(i);
                TextView tvStart = breakItem.findViewById(R.id.tvBreakStart);
                TextView tvEnd = breakItem.findViewById(R.id.tvBreakEnd);

                UserSetupData.BreakTime breakTime = new UserSetupData.BreakTime();
                breakTime.start = tvStart.getText().toString();
                breakTime.end = tvEnd.getText().toString();

                UserSetupData.breakTimes.add(breakTime);
            }

            // מעבר למסך האישור
            ((SetupActivity) requireActivity()).goToNextFragment(new SetupConfirmFragment());
        });

        return view;
    }

    private void showTimePicker(TextView tv) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (TimePicker view, int selectedHour, int selectedMinute) -> {
                    tv.setText(String.format("%02d:%02d", selectedHour, selectedMinute));
                }, hour, minute, true);

        timePickerDialog.show();
    }
}
