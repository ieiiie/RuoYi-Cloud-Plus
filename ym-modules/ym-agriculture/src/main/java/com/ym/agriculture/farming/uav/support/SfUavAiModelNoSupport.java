package com.ym.agriculture.farming.uav.support;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Normalizes AI model numbers while preserving caller order.
 */
public final class SfUavAiModelNoSupport {

    private SfUavAiModelNoSupport() {
    }

    public static List<String> normalize(List<String> modelNos, String fallbackSingle) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (modelNos != null) {
            for (String modelNo : modelNos) {
                addCsvAware(out, modelNo);
            }
        }
        if (out.isEmpty()) {
            addCsvAware(out, fallbackSingle);
        }
        return new ArrayList<>(out);
    }

    public static List<String> split(String csv) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        addCsvAware(out, csv);
        return new ArrayList<>(out);
    }

    public static List<String> splitWithFallback(String csv, String fallbackSingle) {
        List<String> out = split(csv);
        if (!out.isEmpty()) {
            return out;
        }
        return normalize(null, fallbackSingle);
    }

    public static String join(List<String> modelNos) {
        List<String> normalized = normalize(modelNos, null);
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    public static String firstOrNull(List<String> modelNos) {
        List<String> normalized = normalize(modelNos, null);
        return normalized.isEmpty() ? null : normalized.get(0);
    }

    private static void addCsvAware(LinkedHashSet<String> out, String raw) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        for (String part : raw.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
    }
}
