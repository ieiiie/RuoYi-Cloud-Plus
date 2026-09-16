package com.ym.agriculture.farming.news.service;

import com.ym.agriculture.farming.news.model.entity.SfNewsRevision;
import com.ym.agriculture.farming.news.model.vo.SfNewsMediaItemVo;

import java.util.List;

public interface ISfNewsMediaService {
    record EnqueueResult(int queued, int skipped) { }
    EnqueueResult reconcileRevision(SfNewsRevision revision);
    int processPending();
    void wake();
    void retryFailed(Long revisionId);
    List<SfNewsMediaItemVo> listByRevision(Long revisionId);
    void validatePublishable(SfNewsRevision revision);
}
