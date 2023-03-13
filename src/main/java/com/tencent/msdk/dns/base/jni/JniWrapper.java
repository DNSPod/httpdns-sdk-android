package com.tencent.msdk.dns.base.jni;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.NetworkStack;

public final class JniWrapper {
    static {
        try {
            System.loadLibrary("httpdns");
        } catch (Throwable tr) {
            DnsLog.w("Load dns so failed");
        }
    }

    public static int getNetworkStack() {
        try {
            return Jni.getNetworkStack();
        } catch (Throwable tr) {
            DnsLog.w("Get cur network stack failed");
            return NetworkStack.NONE;
        }
    }

    public static int sendToUnity(String strMsg) {
        try {
            return Jni.sendToUnity(strMsg);
        } catch (Throwable tr) {
            DnsLog.w("sendToUnity failed");
            return -4;
        }
    }
}
