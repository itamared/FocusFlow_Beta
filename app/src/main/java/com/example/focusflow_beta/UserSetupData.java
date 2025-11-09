package com.example.focusflow_beta;

import java.util.ArrayList;
import java.util.List;

public class UserSetupData {
    public static String occupation;
    public static String startTime;
    public static String endTime;
    public static int breakCount;
    public static List<BreakTime> breakTimes = new ArrayList<>();

    public static class BreakTime {
        public String start;
        public String end;

        // קונסטרקטור ריק
        public BreakTime() {
        }

        // קונסטרקטור עם פרמטרים
        public BreakTime(String start, String end) {
            this.start = start;
            this.end = end;
        }
    }

}
