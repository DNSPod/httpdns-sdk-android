package com.tencent.msdk.dns.base.jni;

public final class Jni {

    public static native int getNetworkStack();

    public static native int sendToUnity(String strMsg);

}
