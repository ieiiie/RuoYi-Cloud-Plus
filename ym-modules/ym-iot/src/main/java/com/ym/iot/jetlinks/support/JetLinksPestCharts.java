package com.ym.iot.jetlinks.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.iot.device.domain.IotDataPoint;
import com.ym.iot.device.domain.vo.IotPestChartVo;
import com.ym.iot.device.domain.vo.IotPestNightChartVo;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Pure compatibility chart assembly, unchanged pest species/image/night-bucket rules. */
@lombok.extern.slf4j.Slf4j
public final class JetLinksPestCharts {
    private static final int PEST_CHART_QUERY_LIMIT = 2000;
    private static final int PEST_CHART_MAX_SPECIES = 10;
    private static final int PEST_NIGHT_BUCKET_START_HOUR = 21;
    private static final int PEST_NIGHT_BUCKET_END_HOUR = 9;
    private static final String PEST_TOTAL_METRIC = "hfzk_number";
    private static final String PEST_LIST_METRIC = "hfzk_bugerList";
    private static final String PEST_NEW_IMAGE_METRIC = "hfzk_newImage";
    private static final String PEST_IMAGE_METRIC = "hfzk_image";
    private static final String PEST_Y_IMAGE_METRIC = "hfzk_yImage";
    private static final String PEST_OTHER_SPECIES_NAME = "其他";
    private static final DateTimeFormatter PEST_NIGHT_BUCKET_KEY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static IotPestChartVo chart(Long id, List<IotDataPoint> points) {
        NavigableMap<Date, PestRecordAccumulator> records = new TreeMap<>();
        mergeTotalSeries(records, metric(points, PEST_TOTAL_METRIC));
        mergePestItems(records, metric(points, PEST_LIST_METRIC));
        for (String code : List.of(PEST_NEW_IMAGE_METRIC, PEST_IMAGE_METRIC, PEST_Y_IMAGE_METRIC))
            mergeImageSeries(records, metric(points, code), code);
        IotPestChartVo vo = new IotPestChartVo();
        vo.setDeviceId(id);
        vo.setTimes(new ArrayList<>(records.keySet()));
        vo.setTotalSeries(buildPestTotalSeries(records));
        vo.setPestSeries(buildPestStackSeries(records));
        vo.setRecords(buildPestRecords(records));
        vo.setDefaultRecord(resolveDefaultPestRecord(vo.getRecords()));
        return vo;
    }

    public static IotPestNightChartVo night(
            Long id, List<IotDataPoint> points, Date from, Date to) {
        NightChartRange range = resolveNightChartRange(from, to);
        NavigableMap<LocalDateTime, PestNightBucketAccumulator> buckets = new TreeMap<>();
        mergeNightPestItems(buckets, metric(points, PEST_LIST_METRIC), range);
        for (String code : List.of(PEST_NEW_IMAGE_METRIC, PEST_IMAGE_METRIC, PEST_Y_IMAGE_METRIC))
            mergeNightImages(buckets, metric(points, code), range);
        IotPestNightChartVo vo = new IotPestNightChartVo();
        vo.setDeviceId(id);
        vo.setBucketStartHour(PEST_NIGHT_BUCKET_START_HOUR);
        vo.setBucketEndHour(PEST_NIGHT_BUCKET_END_HOUR);
        vo.setBuckets(buildPestNightBuckets(buckets));
        vo.setPestSeries(buildPestNightStackSeries(buckets));
        return vo;
    }

    public static Date[] nightRange(Date from, Date to) {
        var r = resolveNightChartRange(from, to);
        return new Date[] {r.queryFrom(), r.queryTo()};
    }

    private static List<IotDataPoint> metric(List<IotDataPoint> points, String code) {
        return points.stream().filter(p -> code.equals(p.getMetricCode())).toList();
    }

    private static void mergeNightPestItems(
            NavigableMap<LocalDateTime, PestNightBucketAccumulator> buckets,
            List<IotDataPoint> points,
            NightChartRange range) {
        for (IotDataPoint point : points) {
            Date time = resolvePointTime(point);
            if (time == null || StringUtils.isBlank(point.getValueText())) {
                continue;
            }
            LocalDateTime collectTime = toLocalDateTime(time);
            LocalDateTime bucketStart = resolveNightBucketStart(collectTime);
            if (bucketStart == null || !range.intersects(bucketStart)) {
                continue;
            }
            List<IotPestChartVo.PestItem> items = parsePestItems(point.getValueText());
            if (items.isEmpty()) {
                continue;
            }
            PestNightBucketAccumulator bucket =
                    buckets.computeIfAbsent(bucketStart, PestNightBucketAccumulator::new);
            for (IotPestChartVo.PestItem item : items) {
                bucket.mergeItem(item);
            }
        }
    }

    /** 归并夜间图片测点，同一采集时间多个图片字段只计一张照片。 */
    private static void mergeNightImages(
            NavigableMap<LocalDateTime, PestNightBucketAccumulator> buckets,
            List<IotDataPoint> points,
            NightChartRange range) {
        for (IotDataPoint point : points) {
            Date time = resolvePointTime(point);
            if (time == null || !isUsableImageUrl(point.getValueText())) {
                continue;
            }
            LocalDateTime collectTime = toLocalDateTime(time);
            LocalDateTime bucketStart = resolveNightBucketStart(collectTime);
            if (bucketStart == null || !range.intersects(bucketStart)) {
                continue;
            }
            PestNightBucketAccumulator bucket =
                    buckets.computeIfAbsent(bucketStart, PestNightBucketAccumulator::new);
            bucket.photoCollectTimes.add(time);
        }
    }

    private static List<IotPestNightChartVo.NightBucket> buildPestNightBuckets(
            NavigableMap<LocalDateTime, PestNightBucketAccumulator> buckets) {
        if (buckets.isEmpty()) {
            return List.of();
        }
        List<IotPestNightChartVo.NightBucket> out = new ArrayList<>(buckets.size());
        for (PestNightBucketAccumulator bucket : buckets.values()) {
            IotPestNightChartVo.NightBucket vo = new IotPestNightChartVo.NightBucket();
            vo.setBucketKey(bucket.startTime.format(PEST_NIGHT_BUCKET_KEY_FORMATTER));
            vo.setStartTime(toDate(bucket.startTime));
            vo.setEndTime(toDate(bucket.endTime()));
            vo.setLabel(buildNightBucketLabel(bucket.startTime, bucket.endTime()));
            vo.setPhotoCount(bucket.photoCollectTimes.size());
            vo.setTotalCount(bucket.totalCount());
            out.add(vo);
        }
        return out;
    }

    private static List<IotPestNightChartVo.PestStackSeries> buildPestNightStackSeries(
            NavigableMap<LocalDateTime, PestNightBucketAccumulator> buckets) {
        Map<String, Integer> speciesTotals = new LinkedHashMap<>();
        for (PestNightBucketAccumulator bucket : buckets.values()) {
            for (Map.Entry<String, Integer> entry : bucket.speciesCounts.entrySet()) {
                speciesTotals.merge(entry.getKey(), defaultCount(entry.getValue()), Integer::sum);
            }
        }
        if (speciesTotals.isEmpty()) {
            return List.of();
        }

        List<String> topSpecies =
                speciesTotals.entrySet().stream()
                        .sorted(
                                Map.Entry.<String, Integer>comparingByValue(
                                                Comparator.reverseOrder())
                                        .thenComparing(Map.Entry.comparingByKey()))
                        .limit(PEST_CHART_MAX_SPECIES)
                        .map(Map.Entry::getKey)
                        .toList();
        Set<String> topSpeciesSet = new LinkedHashSet<>(topSpecies);
        boolean hasOther =
                speciesTotals.keySet().stream().anyMatch(name -> !topSpeciesSet.contains(name));

        List<IotPestNightChartVo.PestStackSeries> series =
                new ArrayList<>(topSpecies.size() + (hasOther ? 1 : 0));
        for (String speciesName : topSpecies) {
            IotPestNightChartVo.PestStackSeries itemSeries =
                    new IotPestNightChartVo.PestStackSeries();
            itemSeries.setName(speciesName);
            itemSeries.setData(
                    buckets.values().stream()
                            .map(bucket -> defaultCount(bucket.speciesCounts.get(speciesName)))
                            .toList());
            series.add(itemSeries);
        }
        if (hasOther) {
            IotPestNightChartVo.PestStackSeries otherSeries =
                    new IotPestNightChartVo.PestStackSeries();
            otherSeries.setName(PEST_OTHER_SPECIES_NAME);
            otherSeries.setData(
                    buckets.values().stream()
                            .map(bucket -> bucket.otherCount(topSpeciesSet))
                            .toList());
            series.add(otherSeries);
        }
        return series;
    }

    private static NightChartRange resolveNightChartRange(Date from, Date to) {
        Date effectiveTo = to;
        Date effectiveFrom = from;
        if (effectiveFrom == null && effectiveTo == null) {
            LocalDateTime now = LocalDateTime.now();
            effectiveTo = toDate(now);
            effectiveFrom = toDate(now.minusDays(7));
        } else if (effectiveTo == null) {
            effectiveTo = new Date();
        } else if (effectiveFrom == null) {
            effectiveFrom = toDate(toLocalDateTime(effectiveTo).minusDays(7));
        }
        if (effectiveFrom.after(effectiveTo)) {
            throw new ServiceException("开始时间不能晚于结束时间");
        }

        LocalDateTime fromTime = toLocalDateTime(effectiveFrom);
        LocalDateTime toTime = toLocalDateTime(effectiveTo);
        LocalDateTime firstBucketStart = resolveFirstNightBucketStart(fromTime);
        LocalDateTime lastBucketStart = resolveLastNightBucketStart(toTime);
        if (firstBucketStart.isAfter(lastBucketStart)) {
            return new NightChartRange(
                    effectiveFrom, effectiveTo, effectiveFrom, effectiveTo, false);
        }
        Date queryFrom = toDate(firstBucketStart);
        Date queryTo = toDate(lastBucketStart.plusHours(12));
        return new NightChartRange(effectiveFrom, effectiveTo, queryFrom, queryTo, true);
    }

    private static LocalDateTime resolveFirstNightBucketStart(LocalDateTime time) {
        LocalDate date = time.toLocalDate();
        if (time.getHour() < PEST_NIGHT_BUCKET_END_HOUR) {
            date = date.minusDays(1);
        }
        return LocalDateTime.of(date, LocalTime.of(PEST_NIGHT_BUCKET_START_HOUR, 0));
    }

    private static LocalDateTime resolveLastNightBucketStart(LocalDateTime time) {
        LocalDate date = time.toLocalDate();
        if (time.getHour() <= PEST_NIGHT_BUCKET_START_HOUR) {
            date = date.minusDays(1);
        }
        return LocalDateTime.of(date, LocalTime.of(PEST_NIGHT_BUCKET_START_HOUR, 0));
    }

    private static LocalDateTime resolveNightBucketStart(LocalDateTime collectTime) {
        int hour = collectTime.getHour();
        if (hour >= PEST_NIGHT_BUCKET_START_HOUR) {
            return LocalDateTime.of(
                    collectTime.toLocalDate(), LocalTime.of(PEST_NIGHT_BUCKET_START_HOUR, 0));
        }
        if (hour < PEST_NIGHT_BUCKET_END_HOUR) {
            return LocalDateTime.of(
                    collectTime.toLocalDate().minusDays(1),
                    LocalTime.of(PEST_NIGHT_BUCKET_START_HOUR, 0));
        }
        return null;
    }

    private static String buildNightBucketLabel(LocalDateTime startTime, LocalDateTime endTime) {
        return startTime.getMonthValue()
                + "月"
                + startTime.getDayOfMonth()
                + "晚9点~"
                + endTime.getMonthValue()
                + "月"
                + endTime.getDayOfMonth()
                + "早9点";
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }

    /** 合并害虫总数测点，来自 {@code hfzk_number}。 */
    private static void mergeTotalSeries(
            NavigableMap<Date, PestRecordAccumulator> records, List<IotDataPoint> points) {
        for (IotDataPoint point : points) {
            Date time = resolvePointTime(point);
            if (time == null) {
                continue;
            }
            PestRecordAccumulator record =
                    records.computeIfAbsent(time, PestRecordAccumulator::new);
            record.totalCount = toInteger(point.getMetricValue());
        }
    }

    /** 合并虫种明细测点，来自 {@code hfzk_bugerList}。 */
    private static void mergePestItems(
            NavigableMap<Date, PestRecordAccumulator> records, List<IotDataPoint> points) {
        for (IotDataPoint point : points) {
            Date time = resolvePointTime(point);
            if (time == null || StringUtils.isBlank(point.getValueText())) {
                continue;
            }
            List<IotPestChartVo.PestItem> items = parsePestItems(point.getValueText());
            if (items.isEmpty()) {
                records.computeIfAbsent(time, PestRecordAccumulator::new);
                continue;
            }
            PestRecordAccumulator record =
                    records.computeIfAbsent(time, PestRecordAccumulator::new);
            for (IotPestChartVo.PestItem item : items) {
                record.mergeItem(item);
            }
        }
    }

    /** 合并图片测点，图片优先级在构造详情时统一处理。 */
    private static void mergeImageSeries(
            NavigableMap<Date, PestRecordAccumulator> records,
            List<IotDataPoint> points,
            String metricCode) {
        for (IotDataPoint point : points) {
            Date time = resolvePointTime(point);
            if (time == null || StringUtils.isBlank(point.getValueText())) {
                continue;
            }
            PestRecordAccumulator record =
                    records.computeIfAbsent(time, PestRecordAccumulator::new);
            String imageUrl = point.getValueText().trim();
            if (PEST_NEW_IMAGE_METRIC.equals(metricCode)) {
                record.newImage = imageUrl;
            } else if (PEST_IMAGE_METRIC.equals(metricCode)) {
                record.image = imageUrl;
            } else if (PEST_Y_IMAGE_METRIC.equals(metricCode)) {
                record.yImage = imageUrl;
            }
        }
    }

    private static List<IotPestChartVo.TimeValuePoint> buildPestTotalSeries(
            NavigableMap<Date, PestRecordAccumulator> records) {
        List<IotPestChartVo.TimeValuePoint> totalSeries = new ArrayList<>(records.size());
        for (PestRecordAccumulator record : records.values()) {
            IotPestChartVo.TimeValuePoint point = new IotPestChartVo.TimeValuePoint();
            point.setTime(record.collectTime);
            Integer totalCount = record.effectiveTotalCount();
            point.setValue(totalCount != null ? BigDecimal.valueOf(totalCount) : null);
            totalSeries.add(point);
        }
        return totalSeries;
    }

    private static List<IotPestChartVo.PestStackSeries> buildPestStackSeries(
            NavigableMap<Date, PestRecordAccumulator> records) {
        Map<String, Integer> speciesTotals = new LinkedHashMap<>();
        for (PestRecordAccumulator record : records.values()) {
            for (IotPestChartVo.PestItem item : record.items.values()) {
                speciesTotals.merge(item.getName(), defaultCount(item.getCount()), Integer::sum);
            }
        }
        if (speciesTotals.isEmpty()) {
            return List.of();
        }

        List<String> topSpecies =
                speciesTotals.entrySet().stream()
                        .sorted(
                                Map.Entry.<String, Integer>comparingByValue(
                                                Comparator.reverseOrder())
                                        .thenComparing(Map.Entry.comparingByKey()))
                        .limit(PEST_CHART_MAX_SPECIES)
                        .map(Map.Entry::getKey)
                        .toList();
        Set<String> topSpeciesSet = new LinkedHashSet<>(topSpecies);
        boolean hasOther =
                speciesTotals.keySet().stream().anyMatch(name -> !topSpeciesSet.contains(name));

        List<IotPestChartVo.PestStackSeries> series =
                new ArrayList<>(topSpecies.size() + (hasOther ? 1 : 0));
        for (String speciesName : topSpecies) {
            IotPestChartVo.PestStackSeries itemSeries = new IotPestChartVo.PestStackSeries();
            itemSeries.setName(speciesName);
            itemSeries.setData(
                    records.values().stream()
                            .map(record -> defaultCount(record.itemCount(speciesName)))
                            .toList());
            series.add(itemSeries);
        }
        if (hasOther) {
            IotPestChartVo.PestStackSeries otherSeries = new IotPestChartVo.PestStackSeries();
            otherSeries.setName(PEST_OTHER_SPECIES_NAME);
            otherSeries.setData(
                    records.values().stream()
                            .map(record -> record.otherCount(topSpeciesSet))
                            .toList());
            series.add(otherSeries);
        }
        return series;
    }

    private static List<IotPestChartVo.PestRecord> buildPestRecords(
            NavigableMap<Date, PestRecordAccumulator> records) {
        List<IotPestChartVo.PestRecord> out = new ArrayList<>(records.size());
        for (PestRecordAccumulator record : records.values()) {
            IotPestChartVo.PestRecord vo = new IotPestChartVo.PestRecord();
            vo.setCollectTime(record.collectTime);
            vo.setTotalCount(record.effectiveTotalCount());
            vo.setImageUrl(record.imageUrl());
            vo.setItems(new ArrayList<>(record.items.values()));
            out.add(vo);
        }
        return out;
    }

    private static IotPestChartVo.PestRecord resolveDefaultPestRecord(
            List<IotPestChartVo.PestRecord> records) {
        for (int i = records.size() - 1; i >= 0; i--) {
            IotPestChartVo.PestRecord record = records.get(i);
            if (record.getItems() != null && !record.getItems().isEmpty()) {
                return record;
            }
        }
        for (int i = records.size() - 1; i >= 0; i--) {
            IotPestChartVo.PestRecord record = records.get(i);
            if (record.getTotalCount() != null && record.getTotalCount() > 0) {
                return record;
            }
        }
        for (int i = records.size() - 1; i >= 0; i--) {
            IotPestChartVo.PestRecord record = records.get(i);
            if (record.getTotalCount() != null || StringUtils.isNotBlank(record.getImageUrl())) {
                return record;
            }
        }
        return null;
    }

    private static List<IotPestChartVo.PestItem> parsePestItems(String valueText) {
        try {
            JSONArray array = JSON.parseArray(valueText);
            if (array == null || array.isEmpty()) {
                return List.of();
            }
            List<IotPestChartVo.PestItem> items = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                JSONObject object = array.getJSONObject(i);
                if (object == null) {
                    continue;
                }
                String name =
                        firstNotBlank(
                                object.getString("label"),
                                object.getString("name"),
                                object.getString("buggerName"));
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                IotPestChartVo.PestItem item = new IotPestChartVo.PestItem();
                item.setName(name.trim());
                item.setCount(
                        parseCount(
                                firstNotBlank(
                                        object.getString("value"),
                                        object.getString("count"),
                                        object.getString("buggerNum"))));
                item.setRecognizeTime(
                        firstNotBlank(
                                object.getString("date"),
                                object.getString("recognizeTime"),
                                object.getString("createTime")));
                items.add(item);
            }
            return items;
        } catch (JSONException e) {
            log.warn("解析虫情明细失败: {}", e.getMessage());
            return List.of();
        }
    }

    private static Date resolvePointTime(IotDataPoint point) {
        if (point == null) {
            return null;
        }
        return point.getCollectTime() != null ? point.getCollectTime() : point.getReceivedTime();
    }

    private static Integer toInteger(BigDecimal value) {
        return value != null ? value.intValue() : null;
    }

    private static Integer parseCount(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        try {
            return new BigDecimal(value.trim()).intValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int defaultCount(Integer count) {
        return count != null ? count : 0;
    }

    private static String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String firstUsableImage(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (isUsableImageUrl(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isUsableImageUrl(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        String text = value.trim();
        if (text.startsWith("/")) {
            return true;
        }
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            return true;
        }
        try {
            URI uri = URI.create(text);
            String path = uri.getPath();
            return StringUtils.isNotBlank(path) && !"/".equals(path);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 夜间聚合查询范围，保留原始查询区间和扩展后的夜间桶边界。 */
    private record NightChartRange(
            Date from, Date to, Date queryFrom, Date queryTo, boolean hasNightBucket) {

        private boolean intersects(LocalDateTime bucketStart) {
            LocalDateTime bucketEnd = bucketStart.plusHours(12);
            LocalDateTime fromTime = toLocalDateTime(from);
            LocalDateTime toTime = toLocalDateTime(to);
            return bucketEnd.isAfter(fromTime) && bucketStart.isBefore(toTime);
        }
    }

    /** 单个夜间桶的虫种和照片临时聚合结果。 */
    private static class PestNightBucketAccumulator {

        private final LocalDateTime startTime;
        private final Map<String, Integer> speciesCounts = new LinkedHashMap<>();
        private final Set<Date> photoCollectTimes = new LinkedHashSet<>();

        private PestNightBucketAccumulator(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        private LocalDateTime endTime() {
            return startTime.plusHours(12);
        }

        private void mergeItem(IotPestChartVo.PestItem item) {
            if (item == null || StringUtils.isBlank(item.getName())) {
                return;
            }
            speciesCounts.merge(item.getName().trim(), defaultCount(item.getCount()), Integer::sum);
        }

        private int totalCount() {
            int total = 0;
            for (Integer count : speciesCounts.values()) {
                total += defaultCount(count);
            }
            return total;
        }

        private int otherCount(Set<String> topSpecies) {
            int total = 0;
            for (Map.Entry<String, Integer> entry : speciesCounts.entrySet()) {
                if (!topSpecies.contains(entry.getKey())) {
                    total += defaultCount(entry.getValue());
                }
            }
            return total;
        }
    }

    /** 同一采集时间下的虫情测点临时聚合结果。 */
    private static class PestRecordAccumulator {

        private final Date collectTime;
        private final Map<String, IotPestChartVo.PestItem> items = new LinkedHashMap<>();
        private Integer totalCount;
        private String newImage;
        private String image;
        private String yImage;

        private PestRecordAccumulator(Date collectTime) {
            this.collectTime = collectTime;
        }

        private void mergeItem(IotPestChartVo.PestItem source) {
            if (source == null || StringUtils.isBlank(source.getName())) {
                return;
            }
            String name = source.getName().trim();
            IotPestChartVo.PestItem target = items.get(name);
            if (target == null) {
                target = new IotPestChartVo.PestItem();
                target.setName(name);
                target.setCount(defaultCount(source.getCount()));
                target.setRecognizeTime(source.getRecognizeTime());
                items.put(name, target);
                return;
            }
            target.setCount(defaultCount(target.getCount()) + defaultCount(source.getCount()));
            if (StringUtils.isBlank(target.getRecognizeTime())
                    && StringUtils.isNotBlank(source.getRecognizeTime())) {
                target.setRecognizeTime(source.getRecognizeTime());
            }
        }

        private Integer itemCount(String speciesName) {
            IotPestChartVo.PestItem item = items.get(speciesName);
            return item != null ? item.getCount() : 0;
        }

        private int otherCount(Set<String> topSpecies) {
            int total = 0;
            for (IotPestChartVo.PestItem item : items.values()) {
                if (!topSpecies.contains(item.getName())) {
                    total += defaultCount(item.getCount());
                }
            }
            return total;
        }

        private Integer effectiveTotalCount() {
            if (!items.isEmpty()) {
                int total = 0;
                for (IotPestChartVo.PestItem item : items.values()) {
                    total += defaultCount(item.getCount());
                }
                return total;
            }
            return totalCount;
        }

        private String imageUrl() {
            if (items.isEmpty()) {
                return firstUsableImage(image, newImage, yImage);
            }
            return firstUsableImage(newImage, image, yImage);
        }
    }
}
