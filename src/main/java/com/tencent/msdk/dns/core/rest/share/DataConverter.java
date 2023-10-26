package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

public final class DataConverter {

    private static final char[] DIGITS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String bytes2HexString(byte[] bytes) {
        if (null == bytes || 0 == bytes.length) {
            return "";
        }
        char[] buf = new char[2 * bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            buf[2 * i + 1] = DIGITS[b & 0xF];
            b = (byte) (b >>> 4);
            buf[2 * i] = DIGITS[b & 0xF];
        }
        return new String(buf);
    }

    public static byte[] hexString2Bytes(String hexString) {
        if (TextUtils.isEmpty(hexString)) {
            return new byte[0];
        }
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i + 1 < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
