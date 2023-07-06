package com.tencent.msdk.dns.core.rest.aeshttp;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.rest.share.DataConverter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class AesCipherSuite {
    public static final int DEFAULT_IV_LEN = 16;

    private static final String KEY_ALGORITHM = "AES";

    private static final String DEFAULT_VALUE = "0";

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";

    private static final String CHARSET = "utf-8";

    @SuppressWarnings("CharsetObjectCanBeUsed")
    static String encrypt(/* @Nullable */String content, /* @Nullable */String key) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(key)) {
            return "";
        }
        try {
            // get iv from random
            byte[] aesIv = new byte[DEFAULT_IV_LEN];
            SecureRandom random = new SecureRandom();
            random.nextBytes(aesIv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(aesIv);
            // get encrypted content
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(key), ivParameterSpec);
            byte[] encryptByte = cipher.doFinal(content.getBytes(CHARSET));
            // concat iv and encrypted content
            byte[] concatBytes = CommonUtils.concatBytes(aesIv, encryptByte);
            return DataConverter.bytes2HexString(concatBytes);
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
            if (src.length < DEFAULT_IV_LEN) {
                return "";
            }
            // get iv
            byte[] aesIv = new byte[DEFAULT_IV_LEN];
            System.arraycopy(src, 0, aesIv, 0, aesIv.length);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(aesIv);
            // get enc content
            byte[] realContent = new byte[src.length - aesIv.length];
            System.arraycopy(src, aesIv.length, realContent, 0, realContent.length);
            // decrypt
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(key), ivParameterSpec);
            byte[] decryptedBytes = cipher.doFinal(realContent);
            if (decryptedBytes == null) {
                return "";
            }
            return new String(decryptedBytes, Charset.forName(CHARSET));
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * 使用密码获取AES密钥
     */
    public static SecretKeySpec getSecretKey(String secretKey) throws UnsupportedEncodingException {
        String Key = toMakeKey(secretKey);
        return new SecretKeySpec(Key.getBytes(CHARSET), KEY_ALGORITHM);
    }

    /**
     * AES密钥小于默认长度时， 对密钥进行补位，保证密钥安全
     *
     * @param secretKey 密钥key
     * @return 密钥
     */
    private static String toMakeKey(String secretKey) {
        int strLen = secretKey.length();
        if (strLen < AesCipherSuite.DEFAULT_IV_LEN) {
            // 补位
            StringBuilder builder = new StringBuilder();
            builder.append(secretKey);
            for (int i = 0; i < AesCipherSuite.DEFAULT_IV_LEN - strLen; i++) {
                builder.append(AesCipherSuite.DEFAULT_VALUE);
            }
            secretKey = builder.toString();
        }
        return secretKey;
    }
}

