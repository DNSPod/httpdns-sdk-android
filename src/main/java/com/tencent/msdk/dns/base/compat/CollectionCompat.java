package com.tencent.msdk.dns.base.compat;

import android.os.Build;
import android.util.ArrayMap;
import android.util.ArraySet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CollectionCompat {

    @SuppressWarnings("unused")
    public static <K, V> Map<K, V> createMap() {
        // 实现不应该替换为return createMap(0); 因为HashMap两个构造方法实现有所区别
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new ArrayMap<>();
        }
        return new HashMap<>();
    }

    public static <K, V> Map<K, V> createMap(int capacity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new ArrayMap<>(capacity);
        }
        return new HashMap<>(capacity);
    }

    public static <E> Set<E> createSet() {
        // 实现不应该替换为return createSet(0); 因为HashSet两个构造方法实现有所区别
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new ArraySet<>();
        }
        return new HashSet<>();
    }

    public static <E> Set<E> createSet(int capacity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new ArraySet<>(capacity);
        }
        return new HashSet<>(capacity);
    }
}
