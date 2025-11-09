package com.example.focusflow_beta;

import android.app.TimePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

public class WorkHoursFragment extends Fragment {

    private TextView tvStartTime, tvEndTime;
    private Button btnNext;
    private int startHour, startMinute, endHour, endMinute;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_work_hours, container, false);

        tvStartTime = view.findViewById(R.id.tvStartTime);
        tvEndTime = view.findViewById(R.id.tvEndTime);
        btnNext = view.findViewById(R.id.btnNext);

        tvStartTime.setOnClickListener(v -> showTimePicker(true));
        tvEndTime.setOnClickListener(v -> showTimePicker(false));

        btnNext.setOnClickListener(v -> goToNextStep());

        return view;
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(getContext(),
                (TimePicker view, int selectedHour, int selectedMinute) -> {
                    if (isStartTime) {
                        startHour = selectedHour;
                        startMinute = selectedMinute;
                        tvStartTime.setText(String.format("%02d:%02d", startHour, startMinute));
                    } else {
                        endHour = selectedHour;
                        endMinute = selectedMinute;
                        tvEndTime.setText(String.format("%02d:%02d", endHour, endMinute));
                    }
                }, hour, minute, true);
        timePicker.show();
    }

    private void goToNextStep() {
        // שמירה ל-UserSetupData
        UserSetupData.startTime = String.format("%02d:%02d", startHour, startMinute);
        UserSetupData.endTime = String.format("%02d:%02d", endHour, endMinute);

        // מעבר ל-BreakCountFragment
        ((SetupActivity) requireActivity()).goToNextFragment(new BreakCountFragment());
    }
}
