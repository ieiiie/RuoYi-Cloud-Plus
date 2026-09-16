package com.ym.agriculture.farmtask.voice.client;

/**
 * 讯飞机器翻译客户端。
 */
public interface XfyunTranslateClient {

    /**
     * 中文翻译为维语。
     *
     * @param sourceText 中文文本
     * @return 维语文本
     */
    String translateZhToUyghur(String sourceText);
}
