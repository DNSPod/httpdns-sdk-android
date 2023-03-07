package com.tencent.msdk.dns.core.rest.deshttp;

import android.text.TextUtils;

import com.tencent.msdk.dns.core.rest.share.DataConverter;

import java.nio.charset.Charset;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public final class DesCipherSuite {
    private static final String ALGORITHM = "DES";

    private static final String CIPHER_ALGORITHM = "DES/ECB/PKCS5Padding";

    private static final String CHARSET = "utf-8";

    @SuppressWarnings("CharsetObjectCanBeUsed")
    static String encrypt(/* @Nullable */String content, /* @Nullable */String key) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(key)) {
            return "";
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cipher.doFinal(content.getBytes());
            return DataConverter.bytes2HexString(encryptedData);
        } catch (Exception ignored) {
            return "";
        }
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public static String decrypt(/* @Nullable */String content, /* @Nullable */String key) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(key)) {
            return "";
        }
        try {
            byte[] src = DataConverter.hexString2Bytes(content);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(CHARSET), ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(src);
            if (decryptedBytes == null) {
                return "";
            }
            return new String(decryptedBytes, Charset.forName(CHARSET));
        } catch (Exception ignored) {
            return "";
        }
    }
}
