package com.ym.agriculture.farming.bigscreen.support;

/** 大屏根据遥测角度判定阀门开关的轻量阀型。 */
enum BigscreenValveType {
    SINGLE(new int[]{0, 90}),
    THREE_WAY(new int[]{0, 90, 270}),
    FIVE_WAY(new int[]{0, 60, 120, 180, 240, 300, 360});

    static final double DEFAULT_TOLERANCE = 10D;
    private static final double CLOSED_TOLERANCE = 3D;
    private final int[] presets;

    BigscreenValveType(int[] presets) {
        this.presets = presets;
    }

    boolean isOpen(double angle, double tolerance) {
        Integer preset = snap(angle, tolerance);
        if (preset == null) return false;
        return switch (this) {
            case SINGLE -> preset == 90;
            case THREE_WAY -> preset == 90 || preset == 270;
            case FIVE_WAY -> preset == 60 || preset == 120 || preset == 240 || preset == 300;
        };
    }

    boolean isClosed(double angle, double tolerance) {
        Integer preset = snap(angle, tolerance);
        if (preset == null) return false;
        return switch (this) {
            case SINGLE, THREE_WAY -> preset == 0;
            case FIVE_WAY -> preset == 0 || preset == 180 || preset == 360;
        };
    }

    private Integer snap(double angle, double tolerance) {
        if (!Double.isFinite(angle) || tolerance < 0) return null;
        double normalized = ((angle % 360D) + 360D) % 360D;
        int nearest = presets[0];
        double distance = circularDistance(normalized, nearest);
        for (int preset : presets) {
            double candidate = circularDistance(normalized, preset);
            if (candidate < distance) {
                nearest = preset;
                distance = candidate;
            }
        }
        double allowed = isClosedPreset(nearest) ? Math.min(tolerance, CLOSED_TOLERANCE) : tolerance;
        return distance <= allowed ? nearest : null;
    }

    private boolean isClosedPreset(int preset) {
        return switch (this) {
            case SINGLE, THREE_WAY -> preset == 0;
            case FIVE_WAY -> preset == 0 || preset == 180 || preset == 360;
        };
    }

    private static double circularDistance(double left, double right) {
        double diff = Math.abs(left - right);
        return Math.min(diff, 360D - diff);
    }
}
