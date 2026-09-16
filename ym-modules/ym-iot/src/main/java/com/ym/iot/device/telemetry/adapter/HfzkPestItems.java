package com.ym.iot.device.telemetry.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/** The original HFZK adapter's dynamic item contract, shared with compatibility reads. */
public final class HfzkPestItems {
    private HfzkPestItems() { }

    public static JSONArray normalizeVendor(JSONArray items) {
        JSONArray normalized = new JSONArray();
        if (items == null) return normalized;
        for (int i = 0; i < items.size(); i++) {
            JSONObject mapped = vendorItem(items.getJSONObject(i));
            if (mapped != null) normalized.add(mapped);
        }
        return normalized;
    }

    private static JSONObject vendorItem(JSONObject item) {
        if (item == null) return null;
        String name = item.getString("buggerName");
        String num = item.getString("buggerNum");
        if (name == null || name.isBlank()) return null;
        JSONObject mapped = new JSONObject();
        mapped.put("label", name.trim());
        mapped.put("value", num != null ? num.trim() : "0");
        String date = item.getString("createTime");
        if (date != null && !date.isBlank()) mapped.put("date", date.trim());
        return mapped;
    }

    /** Native raw values remain untouched; already-normalized historical values are idempotent. */
    public static JSONArray normalizeRead(Object value) {
        JSONArray items = JSON.parseArray(value instanceof String text ? text : JSON.toJSONString(value));
        JSONArray normalized = new JSONArray();
        if (items == null) return normalized;
        for (int i = 0; i < items.size(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (item != null && item.containsKey("label") && !item.containsKey("buggerName")) {
                normalized.add(new JSONObject(item));
            } else {
                JSONObject mapped = vendorItem(item);
                if (mapped != null) normalized.add(mapped);
            }
        }
        return normalized;
    }
}
