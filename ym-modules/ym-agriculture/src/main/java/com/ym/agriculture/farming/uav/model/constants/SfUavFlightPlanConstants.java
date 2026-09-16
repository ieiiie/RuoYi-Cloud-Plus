package com.ym.agriculture.farming.uav.model.constants;

public final class SfUavFlightPlanConstants {

    private SfUavFlightPlanConstants() {
    }

    public static final String MODE_IMMEDIATE = "IMMEDIATE";
    public static final String MODE_SCHEDULED = "SCHEDULED";
    public static final String MODE_REPEAT = "REPEAT";

    public static final String REPEAT_DAILY = "DAILY";
    public static final String REPEAT_WEEKLY = "WEEKLY";
    public static final String REPEAT_MONTHLY = "MONTHLY";

    public static final String STATUS_PLANNING = "PLANNING";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String EXEC_PENDING = "PENDING";
    public static final String EXEC_SUCCESS = "SUCCESS";
    public static final String EXEC_FAILED = "FAILED";
    public static final String EXEC_SKIPPED = "SKIPPED";

    public static final String EXPIRE_SKIP_EXPIRED = "SKIP_EXPIRED";
}
