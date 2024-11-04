package com.tencent.msdk.dns.core.rest.share;

import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.base.log.DnsLog;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public abstract class AbsHttpDnsConfig {

    private SocketAddress mBizTargetSockAddr = null;

    protected static String SDK_VERSION = "&sdk=1_" + BuildConfig.VERSION_NAME;

    public abstract String getTargetUrl(String dnsIp, String content);

    public abstract int getBizTargetPort();

    /* @Nullable */
    public SocketAddress getTargetSocketAddress(String dnsIp, int family) {
//        if (null == mBizTargetSockAddr) {
            try {
                mBizTargetSockAddr = new InetSocketAddress(InetAddress.getByName(dnsIp), getBizTargetPort());
            } catch (Exception ignored) {
                DnsLog.e("exception: %s", ignored);
            }
//        }
        return mBizTargetSockAddr;
    }
}
