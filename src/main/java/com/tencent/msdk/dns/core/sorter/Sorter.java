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

public final class Sorter implements ISorter {

    private final int mCurNetStack;
    // 适配批量更新的情况，IPFromLocal更改为数组
//    private String mV4IpFromLocal = null;
//    private String mV6IpFromLocal = null;
    private List<String> mV4IpFromLocal = Collections.emptyList();
    private List<String> mV6IpFromLocal = Collections.emptyList();
    private List<String> mV4IpsFromRest = Collections.emptyList();
    private List<String> mV6IpsFromRest = Collections.emptyList();

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
    public IpSet sort() {
        List<String> v4IpSet = new ArrayList<>();
        if (0 != (mCurNetStack & NetworkStack.IPV4_ONLY)) {
            if (!mV4IpsFromRest.isEmpty()) {
                v4IpSet.addAll(mV4IpsFromRest);
            } else if (null != mV4IpFromLocal) {
                v4IpSet.addAll(mV4IpFromLocal);
            }
        }
        List<String> v6IpSet = new ArrayList<>();
        if (0 != (mCurNetStack & NetworkStack.IPV6_ONLY)) {
            if (!mV6IpsFromRest.isEmpty()) {
                v6IpSet.addAll(mV6IpsFromRest);
            } else if (null != mV6IpFromLocal) {
                v6IpSet.addAll(mV6IpFromLocal);
            }
        }
        return new IpSet(v4IpSet.toArray(Const.EMPTY_IPS), v6IpSet.toArray(Const.EMPTY_IPS));
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
