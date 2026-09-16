package com.ym.agriculture.farming.satellite.service;



import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;



import java.util.Optional;



/**

 * 遥感结果影像：从上游 gis-oss URL 下载并转存至系统默认 MinIO。

 */

public interface ISatelliteResultImageTransferService {



    /**

     * 历史迁移单条处理结果。

     */

    enum MigrateOutcome {

        /** 无需处理（已转存或缺少 object_key1） */

        SKIPPED,

        /** 已清除库内非 MinIO 的 oss_url（转存仍未成功） */

        URL_REFRESHED,

        /** 已转存 MinIO 并更新 object_key1 / oss_url */

        MIGRATED,

        /** 无法生成 gis URL 或刷新/转存均失败 */

        FAILED

    }

    /**
     * 存量预览修复单条处理结果。
     */
    enum RepairOutcome {

        /** 无需修复（非 legacy satellite/ 键或无本地 oss_url） */

        SKIPPED,

        /** 已重新标准化上传并更新 object_key1 / oss_url */

        REPAIRED,

        /** 下载或重新上传失败 */

        FAILED

    }



    /**

     * 转存结果。

     *

     * @param localObjectKey 本地 MinIO 对象键（{@code sys_oss.file_name}）

     * @param localUrl       本地公共可访问 URL（写入 {@code oss_url}）

     */

    record TransferResult(String localObjectKey, String localUrl) {

    }



    /**

     * 尝试将上游 {@code objectKey1} 对应影像转存到本地 MinIO；失败返回 empty（不抛异常）。

     */

    Optional<TransferResult> tryTransfer(String tenantId, String dkId, String taskType, String imageDate,

                                         String upstreamObjectKey1, String existingOssUrl);



    /**

     * 历史迁移单条：重新生成 gis URL 写库 → 下载 → 转存 MinIO。

     */

    MigrateOutcome migrateHistoricalResult(SfSatelliteTaskResult result);



    /**

     * 转存单条结果记录（历史迁移用）；内部委托 {@link #migrateHistoricalResult}。

     */

    Optional<TransferResult> tryTransferResult(SfSatelliteTaskResult result);



    /**

     * 清除库内非 MinIO 的 {@code oss_url}（如 gis 阿里云签名链）；不再写入 gis URL。

     */

    void refreshGisOssUrl(SfSatelliteTaskResult result);



    /**

     * 更新结果表中的对象键与 URL。

     */

    void applyTransfer(SfSatelliteTaskResult result, TransferResult transferResult);

    /**
     * 修复 legacy {@code satellite/...} 转存记录的 Web 预览：从本地 {@code oss_url} 下载 → 标准化 → 重新上传。
     */
    RepairOutcome repairPreviewableResult(SfSatelliteTaskResult result);

}
