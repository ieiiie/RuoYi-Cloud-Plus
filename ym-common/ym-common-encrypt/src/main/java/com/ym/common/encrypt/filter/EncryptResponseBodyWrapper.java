package com.ym.common.encrypt.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import com.ym.common.encrypt.utils.EncryptUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密响应参数包装类
 *
 * @author Michelle.Chung
 */
public class EncryptResponseBodyWrapper extends HttpServletResponseWrapper {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Charset RESPONSE_CHARSET = StandardCharsets.UTF_8;

    private final ByteArrayOutputStream byteArrayOutputStream;
    private final ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;

    /**
     * 构造加密响应包装器。
     *
     * @param response 原始响应
     * @throws IOException 创建输出流异常
     */
    public EncryptResponseBodyWrapper(HttpServletResponse response) throws IOException {
        super(response);
        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.servletOutputStream = this.getOutputStream();
    }

    @Override
    public PrintWriter getWriter() {
        if (printWriter == null) {
            printWriter = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, RESPONSE_CHARSET));
        }
        return printWriter;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (servletOutputStream != null) {
            servletOutputStream.flush();
        }
        if (printWriter != null) {
            printWriter.flush();
        }
    }

    @Override
    public void reset() {
        byteArrayOutputStream.reset();
    }

    @Override
    public void resetBuffer() {
        byteArrayOutputStream.reset();
    }

    /**
     * 获取已缓存的响应字节。
     *
     * @return 响应字节数组
     * @throws IOException 刷新响应缓冲异常
     */
    public byte[] getResponseData() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 获取已缓存的响应内容。
     *
     * @return 响应文本
     * @throws IOException 刷新响应缓冲异常
     */
    public String getContent() throws IOException {
        flushBuffer();
        return byteArrayOutputStream.toString(RESPONSE_CHARSET);
    }

    /**
     * 获取加密内容
     *
     * @param servletResponse response
     * @param publicKey       RSA公钥 (用于加密 AES 秘钥)
     * @param headerFlag      请求头标志
     * @return 加密内容
     * @throws IOException
     */
    public String getEncryptContent(HttpServletResponse servletResponse, String publicKey, String headerFlag) throws IOException {
        // 生成秘钥
        String aesPassword = generateAesPassword();
        // 秘钥使用 Base64 编码
        String encryptAes = EncryptUtils.encryptByBase64(aesPassword);
        // Rsa 公钥加密 Base64 编码
        String encryptPassword = EncryptUtils.encryptByRsa(encryptAes, publicKey);

        // 设置响应头
        // vue版本需要设置
        servletResponse.addHeader("Access-Control-Expose-Headers", headerFlag);
        servletResponse.setHeader(headerFlag, encryptPassword);
        servletResponse.setCharacterEncoding(RESPONSE_CHARSET.name());

        // 获取原始内容
        String originalBody = this.getContent();
        // 对内容进行加密
        String encryptContent = EncryptUtils.encryptByAes(originalBody, aesPassword);
        servletResponse.setContentLengthLong(encryptContent.getBytes(RESPONSE_CHARSET).length);
        return encryptContent;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                byteArrayOutputStream.write(b, off, len);
            }
        };
    }

    private String generateAesPassword() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

}
