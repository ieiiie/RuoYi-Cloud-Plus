package com.ym.agriculture.farming.trace.support;

import com.ym.common.core.exception.ServiceException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 可打印溯源标签 PDF（40×60mm 单页标签，批量导出多页）。
 * <p>使用 classpath 中文字体（{@link PDType0Font}），避免标准 14 字体无法编码中文。</p>
 */
public final class TraceLabelPdfRenderer {

    private static final String FONT_REGULAR = "fonts/NotoSansSC-Regular.ttf";
    private static final String FONT_BOLD = "fonts/NotoSansSC-Bold.ttf";

    private static final float MM = 72f / 25.4f;
    private static final float PAGE_W = 40 * MM;
    private static final float PAGE_H = 60 * MM;
    private static final int QR_SIZE = 120;

    private TraceLabelPdfRenderer() {
    }

    public record LabelItem(String qrUrl, String productName, String traceBatchNo, String traceCode) {
    }

    public static byte[] renderSingle(LabelItem item) {
        return renderBatch(List.of(item));
    }

    public static byte[] renderBatch(List<LabelItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("无标签数据可导出");
        }
        try (PDDocument doc = new PDDocument()) {
            PDFont font = loadFont(doc, FONT_REGULAR);
            PDFont fontBold = loadFontIfExists(doc, FONT_BOLD, font);
            for (LabelItem item : items) {
                PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
                doc.addPage(page);
                BufferedImage qr = TraceQrCodeRenderer.render(item.qrUrl(), QR_SIZE);
                byte[] png = toPngBytes(qr);
                PDImageXObject image = PDImageXObject.createFromByteArray(doc, png, "qr");
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float qrDisplay = 28 * MM;
                    float x = (PAGE_W - qrDisplay) / 2;
                    float y = PAGE_H - qrDisplay - 4 * MM;
                    cs.drawImage(image, x, y, qrDisplay, qrDisplay);
                    float textY = y - 3 * MM;
                    drawCentered(cs, fontBold, 9, fitText(item.productName(), 12), textY);
                    textY -= 3.5f * MM;
                    drawCentered(cs, font, 7, fitText(item.traceBatchNo(), 18), textY);
                    textY -= 3.5f * MM;
                    drawCentered(cs, font, 7, TraceCodeGenerator.tailForLabel(item.traceCode()), textY);
                    textY -= 4 * MM;
                    drawCentered(cs, font, 6, "微信扫一扫查看溯源信息", textY);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("生成标签 PDF 失败，请检查中文字体配置：" + e.getMessage());
        }
    }

    private static PDFont loadFont(PDDocument doc, String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new ServiceException("PDF 中文字体文件不存在：" + path
                    + "，请将 NotoSansSC-Regular.ttf 放入 ym-agriculture/src/main/resources/fonts/");
            }
            try (InputStream in = resource.getInputStream()) {
                return PDType0Font.load(doc, in, true);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new ServiceException("加载 PDF 中文字体失败：" + e.getMessage());
        }
    }

    /** 加粗字体可选；缺失时回退为常规字体。 */
    private static PDFont loadFontIfExists(PDDocument doc, String path, PDFont fallback) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return fallback;
        }
        try (InputStream in = resource.getInputStream()) {
            return PDType0Font.load(doc, in, true);
        } catch (IOException e) {
            throw new ServiceException("加载 PDF 中文字体失败：" + e.getMessage());
        }
    }

    private static String fitText(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars - 1) + "…";
    }

    private static void drawCentered(PDPageContentStream cs, PDFont font, float size,
                                     String text, float y) throws IOException {
        if (text == null) {
            text = "";
        }
        float width = font.getStringWidth(text) / 1000 * size;
        float x = (PAGE_W - width) / 2;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(Math.max(2 * MM, x), y);
        cs.showText(text);
        cs.endText();
    }

    private static byte[] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
