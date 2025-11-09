package com.example.focusflow_beta;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.example.focusflow_beta.MainActivity;
import com.example.focusflow_beta.R;
import com.example.focusflow_beta.UserSetupData;

public class SummaryFragment extends Fragment {

    private TextView tvSummary;
    private Button btnConfirm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_summary, container, false);

        tvSummary = view.findViewById(R.id.tvSummary);
        btnConfirm = view.findViewById(R.id.btnConfirm);

        // בניית סיכום מתוך UserSetupData
        StringBuilder summary = new StringBuilder();
        summary.append("עיסוק: ").append(UserSetupData.occupation).append("\n");
        summary.append("טווח שעות: ").append(UserSetupData.startTime)
                .append(" - ").append(UserSetupData.endTime).append("\n");
        summary.append("מספר הפסקות: ").append(UserSetupData.breakCount).append("\n\n");

        for (int i = 0; i < UserSetupData.breakTimes.size(); i++) {
            UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(i);
            summary.append("הפסקה ").append(i + 1).append(": ")
                    .append(bt.start).append(" - ").append(bt.end).append("\n");
        }

        tvSummary.setText(summary.toString());

        btnConfirm.setOnClickListener(v -> {
            // כאן פשוט נעבור למסך הבית
            startActivity(new android.content.Intent(getActivity(), MainActivity.class));
            requireActivity().finish();
        });

        return view;
    }
}
