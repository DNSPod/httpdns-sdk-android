package com.tencent.msdk.dns.base.jni;

public final class Jni {

    public static native int getNetworkStack();

//    unused, plan to remove
//    public static native byte[] desCrypt(byte[] src, String key, int mode);

    public static native byte[] aesCrypt(byte[] src, String key, int mode, byte[] aes_iv);

    public static native int sendToUnity(String strMsg);

}
