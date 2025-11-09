package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OccupationFragment extends Fragment {

    private Button btnStudy, btnWork;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_occupation, container, false);

        TextView title = view.findViewById(R.id.tvTitle);
        btnStudy = view.findViewById(R.id.btnStudy);
        btnWork = view.findViewById(R.id.btnWork);

        // כשנלחץ על "למידה"
        btnStudy.setOnClickListener(v -> {
            UserSetupData.occupation = "למידה";
            ((SetupActivity) requireActivity()).goToNextFragment(new WorkHoursFragment());
        });

        // כשנלחץ על "עבודה"
        btnWork.setOnClickListener(v -> {
            UserSetupData.occupation = "עבודה";
            ((SetupActivity) requireActivity()).goToNextFragment(new WorkHoursFragment());
        });

        return view;
    }
}
