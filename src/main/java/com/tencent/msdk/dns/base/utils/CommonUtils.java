package com.tencent.msdk.dns.base.utils;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.IpSet;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public final class CommonUtils {

    public static void closeQuietly(/* @Nullable */Closeable closeable) {
        if (null != closeable) {
            DnsLog.d("Close %s", closeable);
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static boolean isEmpty(/* @Nullable */Object[] objs) {
        return null == objs || 0 == objs.length;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isEmpty(/* @Nullable */Map<?, ?> map) {
        return null == map || map.isEmpty();
    }

    public static boolean isEmpty(/* @Nullable */Collection<?> collection) {
        return null == collection || collection.isEmpty();
    }

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    public static String toString(/* @Nullable */Object obj) {
        if (null == obj) {
            return "null";
        }
        return obj.toString();
    }

    public static <E> String toString(/* @Nullable */Collection<E> collection) {
        StringBuilder collectionStringBuilder = new StringBuilder("[");
        if (!isEmpty(collection)) {
            for (E element : collection) {
                collectionStringBuilder.append(toString(element)).append(", ");
            }
            // 去掉最后一个", "
            collectionStringBuilder.setLength(collectionStringBuilder.length() - 2);
        }
        collectionStringBuilder.append(']');
        return collectionStringBuilder.toString();
    }

    public static String toStringList(
            /* @Nullable */String[] strs, /* @Nullable */String splitter) {
        if (isEmpty(strs) || TextUtils.isEmpty(splitter)) {
            return "";
        }
        StringBuilder strListBuilder = new StringBuilder();
        for (String str : strs) {
            strListBuilder.append(str).append(splitter);
        }
        if (0 < strListBuilder.length()) {
            strListBuilder.setLength(strListBuilder.length() - 1);
        }
        return strListBuilder.toString();
    }

    public static byte[] concatBytes(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

    public static String getIpfromSet(IpSet ipSet) {
        String v4Ip = "0";
        if (!CommonUtils.isEmpty(ipSet.v4Ips)) {
            v4Ip = ipSet.v4Ips[0];
        }
        String v6Ip = "0";
        if (!CommonUtils.isEmpty(ipSet.v6Ips)) {
            v6Ip = ipSet.v6Ips[0];
        }
        return v4Ip + ";" + v6Ip;
    }
}
