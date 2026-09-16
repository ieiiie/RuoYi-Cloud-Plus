package com.ym.agriculture.farming.trace.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ym.common.core.exception.ServiceException;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * 溯源二维码渲染（H5 跳转 URL）。
 */
public final class TraceQrCodeRenderer {

    private TraceQrCodeRenderer() {
    }

    public static BufferedImage render(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (Exception e) {
            throw new ServiceException("生成二维码失败：" + e.getMessage());
        }
    }
}
