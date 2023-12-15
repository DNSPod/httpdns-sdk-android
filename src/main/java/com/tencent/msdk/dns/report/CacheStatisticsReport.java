package com.tencent.msdk.dns.report;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheStatisticsReport {
    private static final Map<String, Object[]> statisticsMap = new HashMap<>();

    public static void add(LookupResult lookupResult) {
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }
        if (!(lookupResult.stat instanceof StatisticsMerge)) {
            DnsLog.w("lookupResult.stat is not instanceof StatisticsMerge");
            return;
        }
        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;
        String[] hostnameArr;

        if (statMerge.lookupPartCached()) {
            // 对比hostname和requestHostname差值即为部分命中缓存域名数据
            hostnameArr = compare(statMerge.hostname.split(","), statMerge.requestHostname.split(","));
        } else {
            hostnameArr = statMerge.hostname.split(",");
        }

        // 命中缓存的数据，统计上报
        for (String hostname : hostnameArr) {
            if (!statisticsMap.containsKey(hostname)) {
                if (statMerge.lookupSuccess()) {
                    // Object[costTimeMillsTotal, emptyCount, resultCount]
                    statisticsMap.put(hostname, new Object[]{statMerge.restDnsStat.costTimeMills, 0, 1});
                } else {
                    statisticsMap.put(hostname, new Object[]{statMerge.restDnsStat.costTimeMills, 1, 0});
                }

            } else {
                Object[] temp = statisticsMap.get(hostname);
                assert temp != null;
                temp[0] = (Integer) temp[0] + statMerge.restDnsStat.costTimeMills;
                if (statMerge.lookupSuccess()) {
                    temp[2] = (Integer) temp[2] + 1;
                } else {
                    temp[1] = (Integer) temp[1] + 1;
                }
                statisticsMap.put(hostname, temp);
            }
        }
    }

    public static String[] compare(String[] strArr1, String[] strArr2) {
        List<String> list = new ArrayList<>();
        List<String> list2 = Arrays.asList(strArr2);
        for (String str : strArr1) {
            if (!list2.contains(str)) {
                list.add(str);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public static Map<String, Object[]> offerAll() {
        Map<String, Object[]> statistics;
        synchronized (statisticsMap) {
            if (statisticsMap.isEmpty()) {
                statistics = new HashMap<>();
            } else {
                statistics = new HashMap<>(statisticsMap);
                statisticsMap.clear();
            }
        }
        return statistics;
    }

}
