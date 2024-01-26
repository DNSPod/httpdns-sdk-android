package com.tencent.msdk.dns.core.sorter;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.IpValidator;
import com.tencent.msdk.dns.base.utils.NetworkStack;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.ISorter;
import com.tencent.msdk.dns.core.IpSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Sorter implements ISorter {

    private final int mCurNetStack;
    private List<String> mV4IpFromLocal = Collections.emptyList();
    private List<String> mV6IpFromLocal = Collections.emptyList();
    private List<String> mV4IpsFromRest = Collections.emptyList();
    private List<String> mV6IpsFromRest = Collections.emptyList();
    private List<String> mV4IpsFromCache = Collections.emptyList();
    private List<String> mV6IpsFromCache = Collections.emptyList();

    private Sorter(int curNetStack) {
        mCurNetStack = curNetStack;
    }

    @Override
    public synchronized void put(IDns dns, String[] ips) {
        if (null == dns) {
            throw new IllegalArgumentException("dns".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == ips) {
            throw new IllegalArgumentException("ips".concat(Const.NULL_POINTER_TIPS));
        }

        if (CommonUtils.isEmpty(ips)) {
            return;
        }
        if (Const.LOCAL_CHANNEL.equals(dns.getDescription().channel)) {
            DnsLog.d("sorter put lookup from local: %s", Arrays.toString(ips));
            for (String ip : ips) {
                if (IpValidator.isV4Ip(ip)) {
                    mV4IpFromLocal = addIp(mV4IpFromLocal, ip);
                } else if (IpValidator.isV6Ip(ip)) {
                    mV6IpFromLocal = addIp(mV6IpFromLocal, ip);
                }
            }
        } else {
            DnsLog.d("sorter put lookup from rest(%d): %s", dns.getDescription().family, Arrays.toString(ips));
            for (String ip : ips) {
                if (IpValidator.isV4Ip(ip)) {
                    mV4IpsFromRest = addIp(mV4IpsFromRest, ip);
                } else if (IpValidator.isV6Ip(ip)) {
                    mV6IpsFromRest = addIp(mV6IpsFromRest, ip);
                }
            }
        }
    }

    @Override
    public synchronized void putPartCache(IpSet ipSet) {
        String[] v4Ips = ipSet.v4Ips;
        String[] v6Ips = ipSet.v6Ips;
        if (v4Ips != null && v4Ips.length > 0) {
            mV4IpsFromCache = Arrays.asList(v4Ips);
        }
        if (v6Ips != null && v6Ips.length > 0) {
            mV6IpsFromCache = Arrays.asList(v6Ips);
        }
    }

    @Override
    public IpSet sort() {
        String[] v4Ips = Const.EMPTY_IPS;
        if (0 != (mCurNetStack & NetworkStack.IPV4_ONLY)) {
            v4Ips = combineIps("ipv4");
        }
        String[] v6Ips = Const.EMPTY_IPS;
        if (0 != (mCurNetStack & NetworkStack.IPV6_ONLY)) {
            v6Ips = combineIps("ipv6");
        }
        return new IpSet(v4Ips, v6Ips);
    }

    private String[] combineIps(String type) {
        List<String> ipsFromCache = Objects.equals(type, "ipv6") ? mV6IpsFromCache : mV4IpsFromCache;
        List<String> ipsFromLocal = Objects.equals(type, "ipv6") ? mV6IpFromLocal : mV4IpFromLocal;
        List<String> ipsFromRest = Objects.equals(type, "ipv6") ? mV6IpsFromRest : mV4IpsFromRest;
        List<String> ipSet = new ArrayList<>();
        if (!ipsFromCache.isEmpty()) {
            ipSet.addAll(ipsFromCache);
        }

        if (!ipsFromRest.isEmpty()) {
            ipSet.addAll(ipsFromRest);
            if (!ipsFromLocal.isEmpty()) {
                // 对比httpdns和local，对httpdns返回部分域名为空的情况使用localdns兜底
                List<String> restHosts = new ArrayList<>();
                for (String restItem : ipsFromRest) {
                    if (restItem.contains(":")) {
                        String hostname = restItem.split(":")[0];
                        if (!restHosts.contains(hostname)) {
                            restHosts.add(hostname);
                        }
                    }
                }
                for (String item : ipsFromLocal) {
                    if (item.contains(":")) {
                        String hostname = item.split(":")[0];
                        if (!restHosts.contains(hostname)) {
                            DnsLog.d("%s's %s result is from localDns", hostname, type);
                            ipSet.add(item);
                        }
                    }
                }
            }
        } else if (!ipsFromLocal.isEmpty()) {
            DnsLog.d("%s result all from localDns", type);
            ipSet.addAll(ipsFromLocal);
        }
        return ipSet.toArray(Const.EMPTY_IPS);
    }

    private List<String> addIp(List<String> ipList, String ip) {
        if (Collections.<String>emptyList() == ipList) {
            ipList = new ArrayList<>();
        }
        ipList.add(ip);
        return ipList;
    }

    public static class Factory implements IFactory {

        @Override
        public ISorter create(int curNetStack) {
            return new Sorter(curNetStack);
        }
    }
}
