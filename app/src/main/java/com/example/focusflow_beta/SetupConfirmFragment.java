package com.example.focusflow_beta;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetupConfirmFragment extends Fragment {

    private LinearLayout container;
    private Button btnFinish;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup containerParent,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setup_confirm, containerParent, false);

        container = view.findViewById(R.id.confirmContainer);
        btnFinish = view.findViewById(R.id.btnFinishSetup);

        // הצגת הנתונים על המסך
        addText("עיסוק: " + UserSetupData.occupation);
        addText("שעות פעילות: " + UserSetupData.startTime + " - " + UserSetupData.endTime);
        addText("מספר הפסקות: " + UserSetupData.breakCount);

        for (int i = 0; i < UserSetupData.breakTimes.size(); i++) {
            UserSetupData.BreakTime bt = UserSetupData.breakTimes.get(i);
            addText("הפסקה " + (i + 1) + ": " + bt.start + " - " + bt.end);
        }

        // כפתור סיום
        btnFinish.setOnClickListener(v -> {
            saveUserDataToFirestore();
        });

        return view;
    }

    // פונקציה להצגת טקסטים
    private void addText(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(0, 8, 0, 8);
        container.addView(tv);
    }

    // פונקציה לשמירה ל-Firestore
    private void saveUserDataToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // מזהה המשתמש (אם אין auth, ניצור ID אוטומטי)
        String userId = (auth.getCurrentUser() != null)
                ? auth.getCurrentUser().getUid()
                : db.collection("users").document().getId();

        // בניית הנתונים
        Map<String, Object> userData = new HashMap<>();
        userData.put("occupation", UserSetupData.occupation);
        userData.put("startTime", UserSetupData.startTime);
        userData.put("endTime", UserSetupData.endTime);
        userData.put("breakCount", UserSetupData.breakCount);

        List<Map<String, String>> breakList = new ArrayList<>();
        for (UserSetupData.BreakTime bt : UserSetupData.breakTimes) {
            Map<String, String> breakMap = new HashMap<>();
            breakMap.put("start", bt.start);
            breakMap.put("end", bt.end);
            breakList.add(breakMap);
        }
        userData.put("breakTimes", breakList);

        // שמירה למסד הנתונים
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "הנתונים נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();

                    // מעבר למסך הבית אחרי שמירה מוצלחת
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "שגיאה בשמירת הנתונים: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
