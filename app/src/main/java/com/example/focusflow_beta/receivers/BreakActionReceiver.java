package com.example.focusflow_beta.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.focusflow_beta.MainActivity;

public class BreakActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        int breakIndex = intent.getIntExtra("breakIndex", -1);

        context.getSharedPreferences("FocusFlowPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("startBreakNow", true)
                .putInt("breakIndexToStart", breakIndex)
                .apply();

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(openApp);
    }
}
