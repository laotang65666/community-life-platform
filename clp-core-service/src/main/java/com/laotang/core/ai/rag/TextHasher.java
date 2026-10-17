package com.laotang.core.ai.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 文本哈希与归一化。
 */
public final class TextHasher {

    private TextHasher() {
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }

    /**
     * 查询归一化:大小写、首尾空白、连续空白统一,提高缓存命中率。
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
