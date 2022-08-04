package com.tencent.msdk.dns.core.ipRank;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.utils.IpValidator;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.share.AbsRestDns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IpRankHelper {
    private final Set<IpRankItem> ipRankItems = DnsService.getDnsConfig().ipRankItems;
    private Set<String> rankHosts = new HashSet<>();

    /**
     * ipv4优选
     **/
    public void ipv4Rank(String hostname, String[] ips, final IpRankCallback ipRankCallback) {
        // 未配置IP优选，或者当前ip结果长度小于2，不进行优选服务
        if (ipRankItems.isEmpty() || ips.length < 2) {
            return;
        }
        List<String> ipv4Lists = new ArrayList<>();
        for (String ip : ips) {
            if (IpValidator.isV4Ip(ip)) {
                ipv4Lists.add(ip);
            }
        }
        // v4长度小于2，不进行优选服务
        if (ipv4Lists.size() < 2) {
            return;
        }
        if (rankHosts.contains(hostname)) {
            return;
        }
        rankHosts.add(hostname);
        IpRankItem ipRankItem = getIpRankItem(hostname);

        if (ipRankItem != null) {
            // 发起IP测速线程任务
            new Thread(new IpRankTask(hostname, ipv4Lists.toArray(new String[ipv4Lists.size()]), ipRankItem, new IpRankCallback() {
                @Override
                public void onResult(String hostname, String[] sortedIps) {
                    if (ipRankCallback != null) {
                        rankHosts.remove(hostname);
                        ipRankCallback.onResult(hostname, sortedIps);
                    }
                }
            })).start();
        }
    }

    public LookupResult sortResultByIps(String[] sortedIps, LookupResult lookupResult) {
        String[] resultIps = lookupResult.ipSet.ips;
        // 将测速排序的v4ip和v6ip组合
        List<String> sortedAllIps = new ArrayList<>(Arrays.asList(sortedIps));
        for (String ip : resultIps) {
            if (IpValidator.isV6Ip(ip)) {
                sortedAllIps.add(ip);
            }
        }
        String[] ips = sortedAllIps.toArray(new String[sortedAllIps.size()]);
        AbsRestDns.Statistics cachedStat = (AbsRestDns.Statistics) lookupResult.stat;
        cachedStat.ips = ips;
        LookupResult rankLookupResult = new LookupResult<>(ips, cachedStat);
        return rankLookupResult;
    }

    private IpRankItem getIpRankItem(String hostname) {
        if (ipRankItems != null && ipRankItems.size() > 0) {
            for (IpRankItem item : ipRankItems) {
                if (hostname.equals(item.getHostName())) {
                    return item;
                }
            }
        }
        return null;
    }
}
