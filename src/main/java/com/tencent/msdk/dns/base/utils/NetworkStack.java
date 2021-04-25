package com.tencent.msdk.dns.base.utils;

import com.tencent.msdk.dns.base.jni.JniWrapper;

public final class NetworkStack {

    public static final int NONE/* UNKNOWN */ = 0;
    public static final int IPV4_ONLY = 1;
    public static final int IPV6_ONLY = 2;
    public static final int DUAL_STACK = 3;

    public static int get() {
        return JniWrapper.getNetworkStack();
    }

    public static boolean isInvalid(int networkStack) {
        return NONE != networkStack &&
                IPV4_ONLY != networkStack &&
                IPV6_ONLY != networkStack &&
                DUAL_STACK != networkStack;
    }
}
