package com.tencent.msdk.dns.core.local;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.LookupContext;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.stat.AbsStatistics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public final class LocalDns implements IDns<IDns.ILookupExtra> {

    private final DnsDescription mDescription =
            new DnsDescription(Const.LOCAL_CHANNEL, DnsDescription.Family.UN_SPECIFIC);

    /**
     * @hide
     */
    @Override
    public DnsDescription getDescription() {
        return mDescription;
    }

    /**
     * @hide
     */
    @Override
    public LookupResult lookup(LookupParameters lookupParams) {
        if (null == lookupParams) {
            throw new IllegalArgumentException("lookupParams".concat(Const.NULL_POINTER_TIPS));
        }

        Statistics stat = new Statistics();
        stat.startLookup();

        String[] ips = lookup(lookupParams.requestHostname);

        stat.endLookup();
        stat.ips = ips;
        return new LookupResult<>(ips, stat);
    }

    @Override
    public LookupResult getResultFromCache(LookupParameters lookupParams) {
        Statistics stat = new Statistics();
        stat.startLookup();
        stat.endLookup();
        // Local DNS暂不作缓存
        return new LookupResult<>(stat.ips, stat);
    }

    /**
     * @hide
     */
    @Override
    public ISession getSession(LookupContext lookupContext) {
        return null;
    }

    private String[] lookup(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        String[] ips = Const.EMPTY_IPS;
        //  判断是否是批量查询
        String[] hostList = hostname.split(",");
        if (hostList.length > 1) {
            // 批量查询
            ArrayList<String> ipsList = new ArrayList();
            for (String host : hostList) {
                try {
                    // LocalDns使用系统默认时延
                    InetAddress[] inetAddresses = InetAddress.getAllByName(host);
                    ips = new String[inetAddresses.length];
                    for (int i = 0; i < inetAddresses.length; i++) {
                        ipsList.add(host + ":" + inetAddresses[i].getHostAddress());
                    }
                } catch (UnknownHostException e) {
                    DnsLog.d(e, "LocalDns lookup %s failed", host);
                }
            }
            ips = ipsList.toArray(new String[ipsList.size()]);
            // NOTE: 避免无谓的toString调用
            if (DnsLog.canLog(Log.DEBUG)) {
                DnsLog.d("LocalDns lookup for %s result: %s", hostname, Arrays.toString(ips));
            }
        } else {
            try {
                // LocalDns使用系统默认时延
                InetAddress[] inetAddresses = InetAddress.getAllByName(hostname);
                ips = new String[inetAddresses.length];
                for (int i = 0; i < inetAddresses.length; i++) {
                    ips[i] = inetAddresses[i].getHostAddress();
                }
                // NOTE: 避免无谓的toString调用
                if (DnsLog.canLog(Log.DEBUG)) {
                    DnsLog.d("LocalDns lookup for %s result: %s", hostname, Arrays.toString(ips));
                }
            } catch (UnknownHostException e) {
                DnsLog.d(e, "LocalDns lookup %s failed", hostname);
            }
        }

        return ips;
    }

    /**
     * LocalDNS域名解析统计数据类
     */
    public static class Statistics extends AbsStatistics {

        public static final Statistics NOT_LOOKUP = new Statistics();

        @Override
        public String toString() {
            return "Statistics{" +
                    "ips=" + Arrays.toString(ips) +
                    ", costTimeMills=" + costTimeMills +
                    '}';
        }

       @Override
       public boolean lookupPartCached() {
           return false;
       }
    }
}
