package com.ym.agriculture.farming.farmrecord.support;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 农事项目自定义表单模板与提交值校验。
 */
public final class FarmingCustomFormSupport {

    private static final int TEMPLATE_VERSION = 1;
    private static final int DEFAULT_RATING_MAX = 5;
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String FORMAT_DATE = "yyyy-MM-dd";
    private static final String FORMAT_MINUTE = "yyyy-MM-dd HH:mm";
    private static final String FORMAT_SECOND = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_VALUE_TODAY = "TODAY";
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern DATE_MINUTE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$");
    private static final Pattern DATE_SECOND_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");
    private static final Set<String> FIELD_TYPES = Set.of(
        "TEXT",
        "TEXTAREA",
        "SINGLE_SELECT",
        "MULTI_SELECT",
        "IMAGE_UPLOAD",
        "FILE_UPLOAD",
        "DATE",
        "DATETIME_RANGE",
        "NUMBER",
        "RATING"
    );
    private static final Set<String> DATE_FORMATS = Set.of(FORMAT_DATE, FORMAT_MINUTE, FORMAT_SECOND);

    private FarmingCustomFormSupport() {
    }

    public static String normalizeTemplateJson(Map<String, Object> template) {
        if (template == null || template.isEmpty()) {
            return null;
        }
        JSONObject normalized = normalizeTemplateObject(template);
        if (normalized == null) {
            return null;
        }
        return JSON.toJSONString(normalized);
    }

    public static Object parseJson(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JSON.parse(json);
    }

    public static String validateDataJson(String templateJson, Map<String, Object> data) {
        if (StringUtils.isBlank(templateJson)) {
            if (data != null && !data.isEmpty()) {
                throw new ServiceException("未配置自定义表单模板，不能提交自定义表单数据");
            }
            return null;
        }
        JSONObject template = JSON.parseObject(templateJson);
        JSONObject value = data == null ? new JSONObject() : JSON.parseObject(JSON.toJSONString(data));
        validateData(template, value);
        return value.isEmpty() ? null : JSON.toJSONString(value);
    }

    private static JSONObject normalizeTemplateObject(Map<String, Object> template) {
        JSONObject input = JSON.parseObject(JSON.toJSONString(template));
        Integer version = parseInteger(input.get("version"));
        if (version != null && version != TEMPLATE_VERSION) {
            throw new ServiceException("自定义表单模板版本仅支持1");
        }
        Object fieldsObj = input.get("fields");
        if (!(fieldsObj instanceof JSONArray fields)) {
            throw new ServiceException("自定义表单模板fields必须为数组");
        }
        JSONObject out = new JSONObject();
        out.put("version", TEMPLATE_VERSION);
        JSONArray normalizedFields = new JSONArray();
        Set<String> keys = new LinkedHashSet<>();
        for (int i = 0; i < fields.size(); i++) {
            Object rawField = fields.get(i);
            if (!(rawField instanceof JSONObject field)) {
                throw new ServiceException("自定义表单字段必须为对象");
            }
            normalizedFields.add(normalizeField(field, i, keys));
        }
        if (normalizedFields.isEmpty()) {
            return null;
        }
        out.put("fields", normalizedFields);
        return out;
    }

    private static JSONObject normalizeField(JSONObject field, int index, Set<String> keys) {
        String key = trimToNull(field.getString("key"));
        if (StringUtils.isBlank(key)) {
            key = nextGeneratedFieldKey(keys);
        }
        if (!keys.add(key)) {
            throw new ServiceException("自定义表单字段key重复：" + key);
        }
        String label = trimToNull(field.getString("label"));
        if (StringUtils.isBlank(label)) {
            throw new ServiceException("自定义表单字段label不能为空");
        }
        String type = trimToNull(field.getString("type"));
        if (!FIELD_TYPES.contains(type)) {
            throw new ServiceException("不支持的自定义表单字段类型：" + type);
        }

        JSONObject out = new JSONObject();
        out.put("key", key);
        out.put("label", label);
        out.put("type", type);
        out.put("required", Boolean.TRUE.equals(field.getBoolean("required")));
        if (field.containsKey("placeholder")) {
            out.put("placeholder", field.get("placeholder"));
        }
        Object defaultValue = field.get("defaultValue");
        out.put("sortOrder", field.getInteger("sortOrder") == null ? index + 1 : field.getInteger("sortOrder"));
        if (field.get("options") != null) {
            out.put("options", normalizeOptions(type, field.get("options")));
        } else if (requiresOptions(type)) {
            throw new ServiceException("选择类自定义表单字段options不能为空：" + label);
        }
        JSONObject props = normalizeProps(type, field.get("props"));
        JSONObject validation = normalizeValidation(type, field.get("validation"));
        if (field.containsKey("defaultValue")) {
            out.put("defaultValue", normalizeDefaultValue(type, defaultValue, props));
        }
        out.put("props", props);
        out.put("validation", validation);
        return out;
    }

    private static Object normalizeDefaultValue(String type, Object defaultValue, JSONObject props) {
        if (defaultValue == null) {
            return null;
        }
        if (DEFAULT_VALUE_TODAY.equals(defaultValue)) {
            if ("DATE".equals(type) || "DATETIME_RANGE".equals(type)) {
                return DEFAULT_VALUE_TODAY;
            }
            throw new ServiceException("TODAY动态默认值仅支持日期类字段");
        }
        if ("DATE".equals(type)) {
            String text = Objects.toString(defaultValue, null);
            if (StringUtils.isNotBlank(text) && !matchesDateFormat(text, dateFormat(props))) {
                throw new ServiceException("自定义表单默认日期格式无效");
            }
        }
        if ("DATETIME_RANGE".equals(type)) {
            validateDateRangeDefaultValue(defaultValue, dateFormat(props));
        }
        if ("NUMBER".equals(type) && StringUtils.isNotBlank(Objects.toString(defaultValue, null)) && !NumberUtil.isNumber(String.valueOf(defaultValue))) {
            throw new ServiceException("自定义表单数字默认值无效");
        }
        return defaultValue;
    }

    private static JSONArray normalizeOptions(String type, Object rawOptions) {
        if (!requiresOptions(type)) {
            return rawOptions instanceof JSONArray array ? array : new JSONArray();
        }
        if (!(rawOptions instanceof JSONArray options) || options.isEmpty()) {
            throw new ServiceException("选择类自定义表单字段options不能为空");
        }
        JSONArray out = new JSONArray();
        Set<String> values = new LinkedHashSet<>();
        for (Object rawOption : options) {
            if (!(rawOption instanceof JSONObject option)) {
                throw new ServiceException("自定义表单选项必须为对象");
            }
            String label = trimToNull(option.getString("label"));
            String value = trimToNull(option.getString("value"));
            if (StringUtils.isBlank(label) || StringUtils.isBlank(value)) {
                throw new ServiceException("自定义表单选项label和value不能为空");
            }
            if (!values.add(value)) {
                throw new ServiceException("自定义表单选项value重复：" + value);
            }
            JSONObject normalized = new JSONObject();
            normalized.put("label", label);
            normalized.put("value", value);
            out.add(normalized);
        }
        return out;
    }

    private static JSONObject normalizeObject(Object value, String name) {
        if (value == null) {
            return new JSONObject();
        }
        if (!(value instanceof JSONObject object)) {
            throw new ServiceException("自定义表单字段" + name + "必须为对象");
        }
        return object;
    }

    private static JSONObject normalizeProps(String type, Object value) {
        JSONObject props = normalizeObject(value, "props");
        if ("DATE".equals(type) || "DATETIME_RANGE".equals(type)) {
            String format = trimToNull(props.getString("format"));
            if (format == null) {
                props.put("format", DEFAULT_DATE_FORMAT);
            } else if (!DATE_FORMATS.contains(format)) {
                throw new ServiceException("自定义表单日期格式不支持：" + format);
            }
        }
        if ("NUMBER".equals(type) && props.containsKey("unit")) {
            String unit = trimToNull(props.getString("unit"));
            if (unit == null) {
                props.remove("unit");
            } else {
                props.put("unit", unit);
            }
        }
        return props;
    }

    private static JSONObject normalizeValidation(String type, Object value) {
        JSONObject validation = normalizeObject(value, "validation");
        if ("NUMBER".equals(type)) {
            validateNumberRule(validation, "min");
            validateNumberRule(validation, "max");
            Integer precision = validation.getInteger("precision");
            if (precision != null && (precision < 0 || precision > 10)) {
                throw new ServiceException("自定义表单数字小数位范围为0-10");
            }
            Object min = validation.get("min");
            Object max = validation.get("max");
            if (min != null && max != null && Double.parseDouble(String.valueOf(min)) > Double.parseDouble(String.valueOf(max))) {
                throw new ServiceException("自定义表单数字最小值不能大于最大值");
            }
        }
        return validation;
    }

    private static void validateNumberRule(JSONObject validation, String key) {
        Object value = validation.get(key);
        if (value != null && !NumberUtil.isNumber(String.valueOf(value))) {
            throw new ServiceException("自定义表单数字" + key + "必须为数字");
        }
    }

    private static void validateData(JSONObject template, JSONObject data) {
        JSONArray fields = template.getJSONArray("fields");
        if (fields == null) {
            throw new ServiceException("自定义表单模板fields必须为数组");
        }
        Set<String> templateKeys = new LinkedHashSet<>();
        for (Object raw : fields) {
            JSONObject field = (JSONObject) raw;
            String key = field.getString("key");
            templateKeys.add(key);
            Object value = data.get(key);
            if (isEmptyValue(value)) {
                if (Boolean.TRUE.equals(field.getBoolean("required"))) {
                    throw new ServiceException("自定义表单字段不能为空：" + field.getString("label"));
                }
                continue;
            }
            validateFieldValue(field, value);
        }
        for (String key : data.keySet()) {
            if (!templateKeys.contains(key)) {
                throw new ServiceException("自定义表单字段不存在：" + key);
            }
        }
    }

    private static void validateFieldValue(JSONObject field, Object value) {
        String type = field.getString("type");
        switch (type) {
            case "TEXT", "TEXTAREA" -> validateText(field, value);
            case "SINGLE_SELECT" -> validateSingleSelect(field, value);
            case "MULTI_SELECT" -> validateMultiSelect(field, value);
            case "DATE" -> validateDate(field, value);
            case "DATETIME_RANGE" -> validateDateRange(field, value);
            case "NUMBER" -> validateNumber(field, value);
            case "RATING" -> validateRating(field, value);
            case "IMAGE_UPLOAD", "FILE_UPLOAD" -> validateFiles(field, value);
            default -> throw new ServiceException("不支持的自定义表单字段类型：" + type);
        }
    }

    private static void validateText(JSONObject field, Object value) {
        if (!(value instanceof String text)) {
            throw new ServiceException("自定义表单字段必须为文本：" + field.getString("label"));
        }
        Integer maxLength = field.getJSONObject("validation") == null ? null : field.getJSONObject("validation").getInteger("maxLength");
        if (maxLength != null && text.length() > maxLength) {
            throw new ServiceException("自定义表单字段长度不能超过" + maxLength + "：" + field.getString("label"));
        }
    }

    private static void validateSingleSelect(JSONObject field, Object value) {
        String text = String.valueOf(value);
        if (!optionValues(field).contains(text)) {
            throw new ServiceException("自定义表单字段选项无效：" + field.getString("label"));
        }
    }

    private static void validateMultiSelect(JSONObject field, Object value) {
        if (!(value instanceof Collection<?> values)) {
            throw new ServiceException("自定义表单字段必须为数组：" + field.getString("label"));
        }
        Set<String> allowed = optionValues(field);
        for (Object item : values) {
            if (item == null || !allowed.contains(String.valueOf(item))) {
                throw new ServiceException("自定义表单字段选项无效：" + field.getString("label"));
            }
        }
    }

    private static void validateDate(JSONObject field, Object value) {
        if (DEFAULT_VALUE_TODAY.equals(value) || !(value instanceof String text) || !matchesDateFormat(text, dateFormat(field.getJSONObject("props")))) {
            throw new ServiceException("自定义表单字段日期格式无效：" + field.getString("label"));
        }
    }

    private static void validateDateRange(JSONObject field, Object value) {
        if (!(value instanceof Collection<?> values) || values.size() != 2) {
            throw new ServiceException("自定义表单字段日期范围必须为长度为2的数组：" + field.getString("label"));
        }
        String format = dateFormat(field.getJSONObject("props"));
        for (Object item : values) {
            if (DEFAULT_VALUE_TODAY.equals(item) || !(item instanceof String text) || !matchesDateFormat(text, format)) {
                throw new ServiceException("自定义表单字段日期范围格式无效：" + field.getString("label"));
            }
        }
    }

    private static void validateNumber(JSONObject field, Object value) {
        String text = String.valueOf(value);
        if (!NumberUtil.isNumber(text)) {
            throw new ServiceException("自定义表单字段必须为数字：" + field.getString("label"));
        }
        double number = Double.parseDouble(text);
        JSONObject validation = field.getJSONObject("validation");
        if (validation != null) {
            Double min = parseDouble(validation.get("min"));
            Double max = parseDouble(validation.get("max"));
            Integer precision = validation.getInteger("precision");
            if (min != null && number < min) {
                throw new ServiceException("自定义表单字段数字不能小于" + min + "：" + field.getString("label"));
            }
            if (max != null && number > max) {
                throw new ServiceException("自定义表单字段数字不能大于" + max + "：" + field.getString("label"));
            }
            if (precision != null && decimalLength(text) > precision) {
                throw new ServiceException("自定义表单字段数字小数位不能超过" + precision + "：" + field.getString("label"));
            }
        }
    }

    private static void validateRating(JSONObject field, Object value) {
        String text = String.valueOf(value);
        if (!NumberUtil.isNumber(text)) {
            throw new ServiceException("自定义表单字段必须为数字：" + field.getString("label"));
        }
        double number = Double.parseDouble(text);
        Integer max = field.getJSONObject("props") == null ? null : field.getJSONObject("props").getInteger("max");
        int limit = max == null ? DEFAULT_RATING_MAX : max;
        if (number < 0 || number > limit) {
            throw new ServiceException("自定义表单字段评分超出范围：" + field.getString("label"));
        }
    }

    private static void validateFiles(JSONObject field, Object value) {
        if (!(value instanceof Collection<?> files)) {
            throw new ServiceException("自定义表单字段必须为文件数组：" + field.getString("label"));
        }
        Integer maxCount = field.getJSONObject("props") == null ? null : field.getJSONObject("props").getInteger("maxCount");
        if (maxCount != null && files.size() > maxCount) {
            throw new ServiceException("自定义表单字段文件数量不能超过" + maxCount + "：" + field.getString("label"));
        }
        for (Object file : files) {
            if (!(file instanceof Map<?, ?> map) || StringUtils.isBlank(Objects.toString(map.get("url"), null))) {
                throw new ServiceException("自定义表单字段文件url不能为空：" + field.getString("label"));
            }
        }
    }

    private static Set<String> optionValues(JSONObject field) {
        JSONArray options = field.getJSONArray("options");
        Set<String> values = new LinkedHashSet<>();
        if (options != null) {
            for (Object raw : options) {
                values.add(((JSONObject) raw).getString("value"));
            }
        }
        return values;
    }

    private static boolean requiresOptions(String type) {
        return "SINGLE_SELECT".equals(type) || "MULTI_SELECT".equals(type);
    }

    private static String dateFormat(JSONObject props) {
        if (props == null || StringUtils.isBlank(props.getString("format"))) {
            return DEFAULT_DATE_FORMAT;
        }
        return props.getString("format");
    }

    private static boolean matchesDateFormat(String text, String format) {
        return switch (format) {
            case FORMAT_DATE -> DATE_PATTERN.matcher(text).matches();
            case FORMAT_MINUTE -> DATE_MINUTE_PATTERN.matcher(text).matches();
            case FORMAT_SECOND -> DATE_SECOND_PATTERN.matcher(text).matches();
            default -> false;
        };
    }

    private static void validateDateRangeDefaultValue(Object value, String format) {
        if (value == null || DEFAULT_VALUE_TODAY.equals(value)) {
            return;
        }
        if (!(value instanceof Collection<?> values) || values.size() != 2) {
            throw new ServiceException("自定义表单日期范围默认值必须为长度为2的数组");
        }
        for (Object item : values) {
            if (!(item instanceof String text) || !matchesDateFormat(text, format)) {
                throw new ServiceException("自定义表单日期范围默认值格式无效");
            }
        }
    }

    private static Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private static int decimalLength(String text) {
        int index = text.indexOf('.');
        return index < 0 ? 0 : text.length() - index - 1;
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return StringUtils.isBlank(text);
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    private static String trimToNull(String value) {
        return value == null ? null : value.trim();
    }

    private static String nextGeneratedFieldKey(Set<String> keys) {
        int index = 1;
        String key;
        do {
            key = "field-" + String.format("%05d", index);
            index++;
        } while (keys.contains(key));
        return key;
    }

    private static Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        if (!NumberUtil.isLong(text)) {
            throw new ServiceException("自定义表单模板版本仅支持1");
        }
        return Integer.parseInt(text);
    }
}
