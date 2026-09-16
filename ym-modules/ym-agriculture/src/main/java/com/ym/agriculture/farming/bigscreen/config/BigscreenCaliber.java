package com.ym.agriculture.farming.bigscreen.config;

/**
 * 大屏路径 A 业务口径常量。
 *
 * @author ym-cloud
 */
public final class BigscreenCaliber {

    /** 驾驶舱标题 */
    public static final String COCKPIT_TITLE = "轩辕农场运营管理驾驶舱";

    /** 在线率统计范围：租户全部正常 iot_device */
    public static final String ONLINE_RATE_SCOPE = "TENANT_ALL_DEVICES";

    /** 农业传感器设备大类（iot_device.device_category） */
    public static final String SENSOR_DEVICE_CATEGORY = "AG_SENSOR";

    /** 施肥机产品标识（iot_product.product_key） */
    public static final String PRODUCT_KEY_FERTILIZER = "FERTILIZER";

    /** 电动阀产品标识（iot_product.product_key） */
    public static final String PRODUCT_KEY_MOTORVALVE = "MOTORVALVE";

    /** 无人机机场产品标识（iot_product.product_key） */
    public static final String PRODUCT_KEY_UAV_DOCK = "UAV_DOCK";

    /** 气象仪产品标识 */
    public static final String PRODUCT_KEY_WEATHER = "hfzk-1";

    /** 土壤墒情仪产品标识 */
    public static final String PRODUCT_KEY_SOIL = "hfzk-2";

    /** 虫情检测仪产品标识 */
    public static final String PRODUCT_KEY_PEST = "hfzk-3";

    /** 出参：气象传感器子类型 */
    public static final String SENSOR_SUB_WEATHER = "weather";

    /** 出参：土壤传感器子类型 */
    public static final String SENSOR_SUB_SOIL = "soil";

    /** 出参：虫情传感器子类型 */
    public static final String SENSOR_SUB_PEST = "pest";

    /** 出参：未知传感器子类型 */
    public static final String SENSOR_SUB_UNKNOWN = "unknown";

    /** 遥感轮播 task_type 顺序（最多 10 项，与数据清单 §6 一致） */
    public static final String[] RS_TASK_TYPES = {
        "growth", "chlorophyll", "nitrogen", "droughtlevel", "soilmoisture",
        "health", "seedlinggrowth", "bollopening", "cloudcover", "rgb"
    };

    /** 农事时间轴数据源 */
    public static final String TIMELINE_SOURCE = "FARMING_RECORD";

    /** 降雨展示模式（无 % 概率） */
    public static final String RAIN_FORECAST_MODE = "WEATHER_TEXT";

    /** 时间轴默认条数 */
    public static final int TIMELINE_DEFAULT_LIMIT = 20;

    /** 水肥机底栏最近展示天数（按天三罐合计） */
    public static final int FERTILIZER_RECENT_DAY_LIMIT = 7;

    /** 查询原始施肥流水条数上限（合并前） */
    public static final int FERTILIZER_RAW_RECORD_FETCH = 48;

    /** 阀门底栏最近聚合灌溉记录条数 */
    public static final int VALVE_RECENT_HISTORY_LIMIT = 3;

    /** 查询已关阀会话时的抓取上限（用于分钟归并前排序） */
    public static final int VALVE_CLOSED_SESSION_FETCH_SIZE = 50;

    private BigscreenCaliber() {
    }
}
