package com.tencent.msdk.dns.report;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

import java.util.HashMap;
import java.util.Map;

public class CacheStatisticsReport {
    private static Map<String, Object[]> statisticsMap = new HashMap<>();

    public static void add(LookupResult lookupResult) {
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }
        if (!(lookupResult.stat instanceof StatisticsMerge)) {
            DnsLog.w("lookupResult.stat is not instanceof StatisticsMerge");
            return;
        }
        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;

        // 命中缓存的数据，统计上报
        String hostname = statMerge.hostname;
        if (!statisticsMap.containsKey(hostname)) {
            if (lookupResult.stat.lookupSuccess()) {
                // Object[costTimeMillsTotal, emptyCount, resultCount, lookupResult]
                statisticsMap.put(hostname, new Object[]{statMerge.restDnsStat.costTimeMills, 0, 1, lookupResult});
            } else {
                statisticsMap.put(hostname, new Object[]{statMerge.restDnsStat.costTimeMills, 1, 0, lookupResult});
            }

        } else {
            Object[] temp = statisticsMap.get(hostname);
            temp[0] = (Integer) temp[0] + statMerge.restDnsStat.costTimeMills;
            if (lookupResult.stat.lookupSuccess()) {
                temp[2] = (Integer) temp[2] + 1;
            } else {
                temp[1] = (Integer) temp[1] + 1;
            }
            statisticsMap.put(hostname, temp);
        }
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
