package com.tencent.msdk.dns.base.jni;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.NetworkStack;

public final class JniWrapper {
    public static final int ENCRYPTION_MODE = 0;
    public static final int DECRYPTION_MODE = 1;

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

    public static byte[] desCrypt(byte[] src, String key, int mode) {
        try {
            return Jni.desCrypt(src, key, mode);
        } catch (Throwable t) {
            DnsLog.w("dnsCrypt failed");
            return null;
        }
    }

    public static byte[] aesCrypt(byte[] src, String key, int mode, byte[] aes_iv) {
        try {
            return Jni.aesCrypt(src, key, mode, aes_iv);
        } catch (Throwable t) {
            DnsLog.w("dnsCrypt failed");
            return null;
        }
    }
}
