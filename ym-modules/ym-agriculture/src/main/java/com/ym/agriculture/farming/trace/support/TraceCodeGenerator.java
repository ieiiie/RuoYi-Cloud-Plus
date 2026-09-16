package com.ym.agriculture.farming.trace.support;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 溯源码生成：YM + yyyyMMdd + 6位批次短号 + 6位序号 + 2位随机校验。
 */
public final class TraceCodeGenerator {

    private static final String PREFIX = "YM";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final char[] CHECKSUM_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private TraceCodeGenerator() {
    }

    public static String generate(long traceBatchId, int seqNo, LocalDate date) {
        String datePart = (date == null ? LocalDate.now() : date).format(DATE_FMT);
        String batchShort = String.format("%06d", Math.floorMod(traceBatchId, 1_000_000L));
        String seqPart = String.format("%06d", seqNo);
        String checksum = randomChecksum();
        return PREFIX + datePart + batchShort + seqPart + checksum;
    }

    public static List<String> generateBatch(long traceBatchId, int startSeq, int count, LocalDate date) {
        List<String> codes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            codes.add(generate(traceBatchId, startSeq + i, date));
        }
        return codes;
    }

    /** 标签展示用：溯源码后 6–8 位 */
    public static String tailForLabel(String traceCode) {
        if (traceCode == null || traceCode.length() <= 8) {
            return traceCode;
        }
        return traceCode.substring(traceCode.length() - 8);
    }

    private static String randomChecksum() {
        return "" + CHECKSUM_CHARS[RANDOM.nextInt(CHECKSUM_CHARS.length)]
            + CHECKSUM_CHARS[RANDOM.nextInt(CHECKSUM_CHARS.length)];
    }
}
