package com.tencent.msdk.dns.core.rest.aeshttp;

import android.text.TextUtils;
import com.tencent.msdk.dns.base.jni.JniWrapper;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.rest.share.DataConverter;
import java.nio.charset.Charset;
import java.security.SecureRandom;

public final class AesCipherSuite {
  public static final int DEFAULT_IV_LEN = 16;

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
      // get encrypted content
      byte[] rawEncryptContent = JniWrapper
          .aesCrypt(content.getBytes("utf-8"), key, JniWrapper.ENCRYPTION_MODE, aesIv);
      // concat iv and encrypted content
      byte[] concatBytes = CommonUtils.concatBytes(aesIv, rawEncryptContent);
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
      // get enc content
      byte[] realContent = new byte[src.length - aesIv.length];
      System.arraycopy(src, aesIv.length, realContent, 0, realContent.length);
      // decrypt
      byte[] decryptedBytes = JniWrapper.aesCrypt(realContent, key, JniWrapper.DECRYPTION_MODE, aesIv);
      if (decryptedBytes == null) {
        return "";
      }
      return new String(decryptedBytes, Charset.forName("UTF-8"));
    } catch (Exception ignored) {
      return "";
    }
  }
}

