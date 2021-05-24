package com.tencent.msdk.dns.report;

import android.app.Activity;

import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.DnsConfig;
import com.tencent.msdk.dns.base.compat.CollectionCompat;
import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.lifecycle.ActivityLifecycleCallbacksWrapper;
import com.tencent.msdk.dns.base.lifecycle.ActivityLifecycleDetector;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.report.ReportManager;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.IpSet;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.share.AsyncLookupResultQueue;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class ReportHelper {

    private static final long REPORT_ASYNC_LOOKUP_EVENT_INTERVAL_MILLS = 5 * 60 * 1000;

    private static DnsConfig sDnsConfig;

    private static Runnable sReportAsyncLookupEventTask = new Runnable() {

        @Override
        public void run() {
            List<LookupResult> lookupResults = AsyncLookupResultQueue.offerAll();
            reportAsyncLookupEvent(lookupResults);
            DnsExecutors.MAIN.cancel(sReportAsyncLookupEventTask);
            DnsExecutors.MAIN.schedule(
                    sReportAsyncLookupEventTask, REPORT_ASYNC_LOOKUP_EVENT_INTERVAL_MILLS);
        }
    };

    public static void init(DnsConfig dnsConfig) {
        if (null == dnsConfig) {
            throw new IllegalArgumentException("dnsConfig".concat(Const.NULL_POINTER_TIPS));
        }

        sDnsConfig = dnsConfig;
        startReportAsyncLookupEvent();
    }

    public static void reportPreLookupEvent(LookupResult[] lookupResults) {
        if (null == lookupResults) {
            throw new IllegalArgumentException("lookupResults".concat(Const.NULL_POINTER_TIPS));
        }
        if (!ReportManager.canReport()) {
            return;
        }

        // NOTE: 上报字段增减, 记得修改capacity
        Map<String, String> preLookupEventMap = CollectionCompat.createMap(24);

        preLookupEventMap.put(ReportConst.CHANNEL_KEY, sDnsConfig.channel);
        preLookupEventMap.put(ReportConst.LOOKUP_COUNT_KEY, String.valueOf(lookupResults.length));
        BatchStatistics.Builder batchStatBuilder = new BatchStatistics.Builder(false);
        for (LookupResult lookupResult : lookupResults) {
            batchStatBuilder.append((StatisticsMerge) lookupResult.stat);
        }
        BatchStatistics batchStat = batchStatBuilder.build();
        preLookupEventMap.put(ReportConst.BATCH_NETWORK_TYPE_KEY, batchStat.netTypeList);
        preLookupEventMap.put(ReportConst.BATCH_SSID_KEY, batchStat.ssidList);
        preLookupEventMap.put(ReportConst.BATCH_HOSTNAME_KEY, batchStat.hostnameList);
        preLookupEventMap.put(ReportConst.BATCH_NETWORK_STACK_KEY, batchStat.netStackList);

        preLookupEventMap.put(ReportConst.BATCH_REST_INET_LOOKUP_ERROR_CODE_KEY,
                batchStat.restInetLookupErrorCodeList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET_LOOKUP_ERROR_MESSAGE_KEY,
                batchStat.restInetLookupErrorMsgList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET_LOOKUP_IPS_KEY,
                batchStat.restInetLookupIpsList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET_LOOKUP_TTL_KEY,
                batchStat.restInetLookupTtlList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET_LOOKUP_CLIENT_IP_KEY,
                batchStat.restInetLookupClientIpList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET_LOOKUP_COST_TIME_MILLS_KEY,
                batchStat.restInetLookupCostTimeMillsList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET_LOOKUP_RETRY_TIMES_KEY,
                batchStat.restInetLookupRetryTimesList);

        preLookupEventMap.put(ReportConst.BATCH_REST_INET6_LOOKUP_ERROR_CODE_KEY,
                batchStat.restInet6LookupErrorCodeList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET6_LOOKUP_ERROR_MESSAGE_KEY,
                batchStat.restInet6LookupErrorMsgList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET6_LOOKUP_IPS_KEY,
                batchStat.restInet6LookupIpsList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET6_LOOKUP_TTL_KEY,
                batchStat.restInet6LookupTtlList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET6_LOOKUP_CLIENT_IP_KEY,
                batchStat.restInet6LookupClientIpList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET6_LOOKUP_COST_TIME_MILLS_KEY,
                batchStat.restInet6LookupCostTimeMillsList);
        preLookupEventMap.put(ReportConst.BATCH_REST_INET6_LOOKUP_RETRY_TIMES_KEY,
                batchStat.restInet6LookupRetryTimesList);

        addCommonConfigInfo(preLookupEventMap);

        report(ReportConst.PRE_LOOKUP_EVENT_NAME, preLookupEventMap);
    }

    public static void reportLookupMethodCalledEvent(LookupResult lookupResult) {
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }
        if (!ReportManager.canReport()) {
            return;
        }
        if (!(lookupResult.stat instanceof StatisticsMerge)) {
            DnsLog.w("lookupResult.stat is not instanceof StatisticsMerge");
            return;
        }

        // NOTE: 上报字段增减, 记得修改capacity
        Map<String, String> lookupMethodCalledEventMap = CollectionCompat.createMap(29);

        IpSet ipSet = lookupResult.ipSet;
        lookupMethodCalledEventMap.put(ReportConst.INET_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(ipSet.v4Ips, ReportConst.IP_SPLITTER));
        lookupMethodCalledEventMap.put(ReportConst.INET6_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(ipSet.v6Ips, ReportConst.IP_SPLITTER));

        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;

        lookupMethodCalledEventMap.put(ReportConst.CHANNEL_KEY, statMerge.channel);
        lookupMethodCalledEventMap.put(ReportConst.NETWORK_TYPE_KEY, statMerge.netType);
        lookupMethodCalledEventMap.put(ReportConst.SSID_KEY, statMerge.ssid);
        lookupMethodCalledEventMap.put(ReportConst.HOSTNAME_KEY, statMerge.hostname);
        lookupMethodCalledEventMap.put(ReportConst.NETWORK_STACK_KEY,
                String.valueOf(statMerge.curNetStack));

        lookupMethodCalledEventMap.put(ReportConst.LOCAL_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(statMerge.localDnsStat.ips, ReportConst.IP_SPLITTER));
        lookupMethodCalledEventMap.put(ReportConst.LOCAL_LOOKUP_COST_TIME_MILLS_KEY,
                String.valueOf(statMerge.localDnsStat.costTimeMills));

        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_CACHE_HIT_KEY,
                String.valueOf(statMerge.restInetDnsStat.cached));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_ERROR_CODE_KEY,
                String.valueOf(statMerge.restInetDnsStat.errorCode));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_ERROR_MESSAGE_KEY,
                statMerge.restInetDnsStat.errorMsg);
        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(statMerge.restInetDnsStat.ips, ReportConst.IP_SPLITTER));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_TTL_KEY,
                String.valueOf(statMerge.restInetDnsStat.ttl));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_CLIENT_IP_KEY,
                statMerge.restInetDnsStat.clientIp);
        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_COST_TIME_MILLS_KEY,
                String.valueOf(statMerge.restInetDnsStat.costTimeMills));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET_LOOKUP_RETRY_TIMES_KEY,
                String.valueOf(statMerge.restInetDnsStat.retryTimes));

        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_CACHE_HIT_KEY,
                String.valueOf(statMerge.restInet6DnsStat.cached));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_ERROR_CODE_KEY,
                String.valueOf(statMerge.restInet6DnsStat.errorCode));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_ERROR_MESSAGE_KEY,
                statMerge.restInet6DnsStat.errorMsg);
        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(statMerge.restInet6DnsStat.ips, ReportConst.IP_SPLITTER));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_TTL_KEY,
                String.valueOf(statMerge.restInet6DnsStat.ttl));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_CLIENT_IP_KEY,
                statMerge.restInet6DnsStat.clientIp);
        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_COST_TIME_MILLS_KEY,
                String.valueOf(statMerge.restInet6DnsStat.costTimeMills));
        lookupMethodCalledEventMap.put(ReportConst.REST_INET6_LOOKUP_RETRY_TIMES_KEY,
                String.valueOf(statMerge.restInet6DnsStat.retryTimes));

        addCommonConfigInfo(lookupMethodCalledEventMap);

        report(ReportConst.LOOKUP_METHOD_CALLED_EVENT_NAME, lookupMethodCalledEventMap);
    }

    private static void startReportAsyncLookupEvent() {
        // NOTE: 无论是否存在可用的上报通道, 都应该不断消费异步解析结果
        DnsExecutors.MAIN.schedule(
                sReportAsyncLookupEventTask, REPORT_ASYNC_LOOKUP_EVENT_INTERVAL_MILLS);
        ActivityLifecycleDetector.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksWrapper() {
            @Override
            public void onActivityStopped(Activity activity) {
                DnsExecutors.MAIN.execute(sReportAsyncLookupEventTask);
            }
        });
    }

    private static void reportAsyncLookupEvent(Collection<LookupResult> lookupResults) {
        if (CommonUtils.isEmpty(lookupResults) || !ReportManager.canReport()) {
            return;
        }

        // NOTE: 上报字段增减, 记得修改capacity
        Map<String, String> asyncLookupEventMap = CollectionCompat.createMap(19);

        asyncLookupEventMap.put(ReportConst.CHANNEL_KEY, sDnsConfig.channel);
        asyncLookupEventMap.put(ReportConst.LOOKUP_COUNT_KEY, String.valueOf(lookupResults.size()));
        BatchStatistics.Builder batchStatBuilder = new BatchStatistics.Builder(true);
        for (LookupResult lookupResult : lookupResults) {
            batchStatBuilder.append((StatisticsMerge) lookupResult.stat);
        }
        BatchStatistics batchStat = batchStatBuilder.build();
        asyncLookupEventMap.put(ReportConst.BATCH_NETWORK_TYPE_KEY, batchStat.netTypeList);
        asyncLookupEventMap.put(ReportConst.BATCH_SSID_KEY, batchStat.ssidList);
        asyncLookupEventMap.put(
                ReportConst.BATCH_NETWORK_CHANGE_KEY, batchStat.restInetNetChangeLookupList);
        asyncLookupEventMap.put(ReportConst.BATCH_HOSTNAME_KEY, batchStat.hostnameList);
        asyncLookupEventMap.put(
                ReportConst.BATCH_LOOKUP_TIME_MILLS_KEY, batchStat.restInetStartLookupTimeMillsList);
        asyncLookupEventMap.put(ReportConst.BATCH_NETWORK_STACK_KEY, batchStat.netStackList);

        asyncLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_ERROR_CODE_KEY,
                batchStat.restInetLookupErrorCodeList);
        asyncLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_ERROR_MESSAGE_KEY,
                batchStat.restInetLookupErrorMsgList);
        asyncLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_IPS_KEY,
                batchStat.restInetLookupIpsList);
        asyncLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_TTL_KEY,
                batchStat.restInetLookupTtlList);
        asyncLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_CLIENT_IP_KEY,
                batchStat.restInetLookupClientIpList);
        asyncLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_COST_TIME_MILLS_KEY,
                batchStat.restInetLookupCostTimeMillsList);
        asyncLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_RETRY_TIMES_KEY,
                batchStat.restInetLookupRetryTimesList);

        addCommonConfigInfo(asyncLookupEventMap);

        report(ReportConst.ASYNC_LOOKUP_EVENT_NAME, asyncLookupEventMap);
    }

    private static void addCommonConfigInfo(Map<String, String> eventMap) {
        eventMap.put(ReportConst.SDK_VERSION_KEY, BuildConfig.VERSION_NAME);
        eventMap.put(ReportConst.APP_ID_KEY, sDnsConfig.appId);
        eventMap.put(ReportConst.BIZ_ID_KEY, sDnsConfig.lookupExtra.bizId);
        eventMap.put(ReportConst.USER_ID_KEY, sDnsConfig.userId);
    }

    private static void report(String eventName, Map<String, String> eventMap) {
        ReportManager.report(ReportManager.Environment.TEST | ReportManager.Environment.RELEASE,
                eventName, eventMap);
    }
}
