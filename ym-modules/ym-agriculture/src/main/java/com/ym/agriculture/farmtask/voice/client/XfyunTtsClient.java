package com.ym.agriculture.farmtask.voice.client;

/**
 * 讯飞理想语音合成客户端。
 */
public interface XfyunTtsClient {

    /**
     * 合成维语音频。
     *
     * @param uyghurText 维语文本
     * @return 音频字节
     */
    byte[] synthesizeUyghur(String uyghurText);
}
