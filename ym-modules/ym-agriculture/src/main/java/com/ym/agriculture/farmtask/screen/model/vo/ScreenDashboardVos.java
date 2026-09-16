package com.ym.agriculture.farmtask.screen.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/** 大屏各区域最小化响应模型。 */
public final class ScreenDashboardVos {

    private ScreenDashboardVos() {
    }

    @Data
    public static class Tenant {
        private String tenantId;
        private String tenantName;
    }

    @Data
    public static class Greenhouses {
        private long total;
        /** 至少存在一张有效巡查或农事档案图片的大棚数量。 */
        private long withPhotoArchiveCount;
        private List<GreenhouseColumn> columns;
        private List<Greenhouse> unplacedRows;
        /** 兼容旧版大屏；后续单独评审移除。 */
        private List<Greenhouse> rows;
    }

    @Data
    public static class GreenhouseColumn {
        private String columnName;
        private Integer columnOrder;
        private List<Greenhouse> rows;
    }

    @Data
    public static class Greenhouse {
        private Long fieldId;
        private String fieldCode;
        private String fieldName;
        private String greenhouseShortName;
        private String greenhouseColor;
        private String speciesName;
        private String speciesImageUrl;
        private Integer sortOrder;
        private Integer rowOrder;
        private String statusDisplay;
        private BigDecimal areaMu;
        /** 巡查档案与农事档案完工、验收图片的有效图片总数。 */
        private long photoArchiveCount;
    }

    @Data
    public static class PhotoArchives {
        private Long fieldId;
        private String fieldName;
        private List<PhotoArchive> archives;
    }

    @Data
    public static class PhotoArchive {
        private Long archiveId;
        private LocalDate archiveDate;
        private List<Photo> photos;
    }

    @Data
    public static class Photo {
        private Long photoId;
        private String originalName;
        private String url;
        private Date uploadedAt;
    }

    @Data
    public static class FarmArchiveDates {
        private Long fieldId;
        private String fieldName;
        private List<FarmArchiveDate> dates;
    }

    @Data
    public static class FarmArchiveDate {
        private LocalDate archiveDate;
        private long taskCount;
    }

    @Data
    public static class FarmArchiveTasks {
        private Long fieldId;
        private String fieldName;
        private LocalDate archiveDate;
        private List<FarmArchiveTask> tasks;
    }

    @Data
    public static class FarmArchiveTask {
        private String workItemName;
        private String leaderName;
        private List<FarmPhoto> photos;
    }

    @Data
    public static class FarmPhoto {
        private Long photoId;
        private String originalName;
        private String url;
    }

    @Data
    public static class Sop {
        private Long sopId;
        private String workItemName;
        private String cropScope;
        private String cropSpeciesName;
        private String language;
        private Date updateTime;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class SopDetail extends Sop {
        private List<?> contentBlocks;
    }

    @Data
    public static class Material {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String category;
        private BigDecimal stockQuantity;
        private String unit;
        private Date updateTime;
    }

    @Data
    public static class WorkOrderSummary {
        private long pendingCount;
        private long processingCount;
        private long pendingAcceptanceCount;
        private long completedTodayCount;
    }

    @Data
    public static class WorkOrderCategoryRanking {
        private List<WorkOrderCategoryRank> rows;
        private String caliber;
    }

    @Data
    public static class WorkOrderCategoryRank {
        private int rank;
        private String categoryName;
        private long orderCount;
    }

    @Data
    public static class WorkOrderWorkItemRanking {
        private List<WorkOrderWorkItemRank> rows;
        private String caliber;
    }

    @Data
    public static class WorkOrderWorkItemRank {
        private int rank;
        private String workItemName;
        private long orderCount;
    }

    @Data
    public static class TodayLabor {
        private LocalDate workDate;
        private BigDecimal workerCount;
        private String unit;
        private String caliber;
    }

    @Data
    public static class CumulativeLabor {
        private BigDecimal workerCount;
        private String unit;
        private String caliber;
    }

    @Data
    public static class AnnualYield {
        private int year;
        private LocalDate startDate;
        private LocalDate endDate;
        private String unit;
        private List<AnnualYieldRow> rows;
    }

    @Data
    public static class AnnualYieldRow {
        private Long varietyId;
        private String varietyName;
        private BigDecimal yieldKg;
    }
}
