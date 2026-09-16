package com.ym.agriculture.farming.market.model.constants;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 农业行情领域固定编码。 */
public final class SfMarketConstants {

    public static final String INGEST_PENDING = "PENDING";
    public static final String INGEST_PROCESSING = "PROCESSING";
    public static final String INGEST_ACCEPTED = "ACCEPTED";
    public static final String INGEST_UNCHANGED = "UNCHANGED";
    public static final String INGEST_REJECTED = "REJECTED";
    public static final String INGEST_FAILED = "FAILED";
    public static final String INGEST_IGNORED = "IGNORED";
    public static final String QUOTE_MARKET = "MARKET";
    public static final String QUOTE_OFFICIAL_AVERAGE = "OFFICIAL_AVERAGE";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String TREND_UP = "up";
    public static final String TREND_DOWN = "down";
    public static final String TREND_FLAT = "flat";
    public static final String TREND_UNKNOWN = "unknown";

    public static final Set<String> CATEGORIES = Set.of("grain", "vegetable", "fruit", "livestock", "aquatic");
    public static final Map<String, String> CATEGORY_NAMES = Map.of(
        "grain", "粮食", "vegetable", "蔬菜", "fruit", "水果", "livestock", "畜牧", "aquatic", "水产");
    public static final Map<String, String> CATEGORY_ALIASES = Map.ofEntries(
        Map.entry("grain", "grain"), Map.entry("粮食", "grain"), Map.entry("粮油", "grain"),
        Map.entry("vegetable", "vegetable"), Map.entry("蔬菜", "vegetable"),
        Map.entry("fruit", "fruit"), Map.entry("水果", "fruit"), Map.entry("果品", "fruit"),
        Map.entry("livestock", "livestock"), Map.entry("畜牧", "livestock"), Map.entry("畜禽", "livestock"),
        Map.entry("肉类", "livestock"), Map.entry("aquatic", "aquatic"), Map.entry("水产", "aquatic"));
    public static final List<String> CATEGORY_ORDER = List.of("grain", "vegetable", "fruit", "livestock", "aquatic");

    private SfMarketConstants() {
    }
}
