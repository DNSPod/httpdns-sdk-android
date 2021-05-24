package com.tencent.msdk.dns.core.rest.deshttp;

import android.text.TextUtils;
import com.tencent.msdk.dns.base.jni.JniWrapper;
import com.tencent.msdk.dns.core.rest.share.DataConverter;
import java.nio.charset.Charset;

final class DesCipherSuite {
    @SuppressWarnings("CharsetObjectCanBeUsed")
    static String encrypt(/* @Nullable */String content, /* @Nullable */String key) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(key)) {
            return "";
        }
        try {
            byte[] rawEncryptContent = JniWrapper
                    .desCrypt(content.getBytes("utf-8"), key, JniWrapper.ENCRYPTION_MODE);
            return DataConverter.bytes2HexString(rawEncryptContent);
        } catch (Exception ignored) {
            return "";
        }
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    static String decrypt(/* @Nullable */String content, /* @Nullable */String key) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(key)) {
            return "";
        }
        try {
            byte[] src = DataConverter.hexString2Bytes(content);
            byte[] decryptedBytes = JniWrapper.desCrypt(src, key, JniWrapper.DECRYPTION_MODE);
            if (decryptedBytes == null) {
                return "";
            }
            return new String(decryptedBytes, Charset.forName("UTF-8"));
        } catch (Exception ignored) {
            return "";
        }
    }
}
