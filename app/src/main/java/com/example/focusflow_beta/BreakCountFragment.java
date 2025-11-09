package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import androidx.fragment.app.Fragment;

public class BreakCountFragment extends Fragment {

    private NumberPicker numberPicker;
    private Button btnNext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_break_count, container, false);

        numberPicker = view.findViewById(R.id.numberPickerBreaks);
        btnNext = view.findViewById(R.id.btnNextBreaks);

        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(5);

        btnNext.setOnClickListener(v -> {
            int breakCount = numberPicker.getValue();

            // שמירה ל־UserSetupData
            UserSetupData.breakCount = breakCount;

            // מעבר לשלב הבא – בחירת שעות הפסקות
            ((SetupActivity) requireActivity()).goToNextFragment(new BreakTimeFragment());
        });

        return view;
    }
}
