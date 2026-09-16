package com.ym.agriculture.farming.bigscreen.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenUavPhotoVo;
import com.ym.agriculture.farming.uav.model.entity.SfUavMediaFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 无人机航拍缩略图组装：排序、非连续抽帧、URL 解析。
 */
@Component
public class BigscreenUavPhotoAssembler {

    /**
     * 按拍摄时间升序排列，便于稳定非连续抽帧。
     */
    public List<SfUavMediaFile> sortImagesForPick(List<SfUavMediaFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
            .sorted(Comparator.comparing(SfUavMediaFile::getFileCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    /**
     * 从影像列表非连续抽取最多 3 张缩略图。
     *
     * @param images 已排除 VIDEO 的图片列表
     */
    public List<SfBigscreenUavPhotoVo> pickPhotos(List<SfUavMediaFile> images) {
        List<SfUavMediaFile> sorted = sortImagesForPick(images);
        if (sorted.isEmpty()) {
            return List.of();
        }
        int size = sorted.size();
        int[] indexes = size >= 3
            ? new int[] {0, size / 3, (2 * size) / 3}
            : new int[] {0};
        List<SfBigscreenUavPhotoVo> photos = new ArrayList<>();
        for (int index : indexes) {
            if (index >= size) {
                continue;
            }
            SfUavMediaFile media = sorted.get(index);
            SfBigscreenUavPhotoVo photo = new SfBigscreenUavPhotoVo();
            photo.setMediaId(media.getUavMediaId());
            String url = resolveMediaUrl(media);
            photo.setImageUrl(url);
            photo.setThumbUrl(url);
            photo.setLabel(media.getFileName());
            photos.add(photo);
        }
        return photos;
    }

    /**
     * 优先 {@code object_key}（飞控落库的完整 OSS URL）；{@code file_path} 多为航线目录前缀，仅作兜底。
     */
    static String resolveMediaUrl(SfUavMediaFile media) {
        if (media == null) {
            return null;
        }
        if (StringUtils.isNotBlank(media.getObjectKey())) {
            return media.getObjectKey().trim();
        }
        return StringUtils.isNotBlank(media.getFilePath()) ? media.getFilePath().trim() : null;
    }
}
