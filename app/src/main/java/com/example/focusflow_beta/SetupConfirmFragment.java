package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import android.content.Intent;

public class SetupConfirmFragment extends Fragment {

    private LinearLayout container;
    private Button btnFinish;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup containerParent,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setup_confirm, containerParent, false);

        container = view.findViewById(R.id.confirmContainer);
        btnFinish = view.findViewById(R.id.btnFinishSetup);

        // הצגת הנתונים
        addText("עיסוק: " + UserSetupData.occupation);
        addText("שעות פעילות: " + UserSetupData.startTime + " - " + UserSetupData.endTime);
        addText("מספר הפסקות: " + UserSetupData.breakCount);

        for (int i = 0; i < UserSetupData.breakTimes.size(); i++) {
            UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(i);
            addText("הפסקה " + (i + 1) + ": " + bt.start + " - " + bt.end);
        }

        // לחיצה על סיום – כניסה ל־MainActivity
        btnFinish.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void addText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(0, 8, 0, 8);
        container.addView(tv);
    }
}
