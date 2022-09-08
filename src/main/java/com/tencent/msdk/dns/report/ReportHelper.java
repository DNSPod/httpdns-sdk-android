package com.tencent.msdk.dns.report;

import android.app.Activity;
import android.content.Context;

import com.tencent.msdk.dns.BackupResolver;
import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.DnsConfig;
import com.tencent.msdk.dns.DnsService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReportHelper {

    private static final long REPORT_ASYNC_LOOKUP_EVENT_INTERVAL_MILLS = 5 * 60 * 1000;

    private static DnsConfig sDnsConfig;

    private static Map<String, Object[]> statisticsMap = new HashMap<>();

    private static Runnable sReportAsyncLookupEventTask = new Runnable() {

        @Override
        public void run() {
            List<LookupResult> lookupResults = AsyncLookupResultQueue.offerAll();
            reportAsyncLookupEvent(lookupResults);
            // attta上报统计数据
            reportStatisticsEvent();
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

        // sessionId初始化
        Session.setSessionId();

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
        preLookupEventMap.put(ReportConst.BATCH_HOSTNAME_KEY, batchStat.hostnameList);
        preLookupEventMap.put(ReportConst.BATCH_NETWORK_STACK_KEY, batchStat.netStackList);

        preLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_ERROR_CODE_KEY,
                batchStat.restInetLookupErrorCodeList);
        preLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_ERROR_MESSAGE_KEY,
                batchStat.restInetLookupErrorMsgList);
        preLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_IPS_KEY,
                batchStat.restInetLookupIpsList);
        preLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_TTL_KEY,
                batchStat.restInetLookupTtlList);
        preLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_CLIENT_IP_KEY,
                batchStat.restInetLookupClientIpList);
        preLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_COST_TIME_MILLS_KEY,
                batchStat.restInetLookupCostTimeMillsList);
        preLookupEventMap.put(ReportConst.BATCH_REST_LOOKUP_RETRY_TIMES_KEY,
                batchStat.restInetLookupRetryTimesList);

        addCommonConfigInfo(preLookupEventMap);

        report(ReportConst.PRE_LOOKUP_EVENT_NAME, preLookupEventMap);
    }

    public static void reportLookupMethodCalledEvent(LookupResult lookupResult, Context context) {
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }
        if (!(lookupResult.stat instanceof StatisticsMerge)) {
            DnsLog.w("lookupResult.stat is not instanceof StatisticsMerge");
            return;
        }
        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;
        DnsLog.d("lookupResult:" + String.valueOf(lookupResult));
        String reqType = null;
        switch (statMerge.curNetStack) {
            case 1:
                reqType = "a";
                break;
            case 2:
                reqType = "aaaa";
                break;
            case 3:
                reqType = "dual";
                break;
        }

        // 命中缓存的数据，统计上报
        if (statMerge.restDnsStat.cached) {
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

        //  ErrorCode==2 进行容灾处理
        if (statMerge.restDnsStat.errorCode == 2 || (Const.HTTPS_CHANNEL.equals(sDnsConfig.channel) && (statMerge.restDnsStat.errorCode == 1))) {
            BackupResolver backupInfo = BackupResolver.getInstance();
            //  仅当达到最大失败次数满足切换IP时候上报
            if (sDnsConfig.enableReport && backupInfo.getCanReport(backupInfo.getErrorCount() + 1)) {
                //  获取手机卡运营商code
                String carrierCode = AttaHelper.getSimOperator(context);
                //  获取当前dnsip
                String dnsIp = backupInfo.getDnsIp();
                //  上报a / aaaa 解析失败
                if (statMerge.restDnsStat.errorCode != 0) {
                    DnsExecutors.MAIN.execute(AttaHelper.report(carrierCode, statMerge.netType, sDnsConfig.lookupExtra.bizId, sDnsConfig.channel, "HttpDnsfail", System.currentTimeMillis(), dnsIp, BuildConfig.VERSION_NAME, AttaHelper.getSystemModel(), "Andriod", AttaHelper.getSystemVersion(), statMerge.restDnsStat.costTimeMills, statMerge.hostname, reqType, sDnsConfig.timeoutMills, statMerge.restDnsStat.ttl, statMerge.restDnsStat.errorCode, statMerge.restDnsStat.statusCode, statMerge.restDnsStat.cached, 1, CommonUtils.toStringList(statMerge.localDnsStat.ips, ReportConst.IP_SPLITTER), CommonUtils.toStringList(statMerge.restDnsStat.ips, ReportConst.IP_SPLITTER)));
                }
            }
            // 报错记录+1
            backupInfo.incrementErrorCount();
            DnsLog.d("dnsip连接失败, 当前失败次数：" + backupInfo.getErrorCount());
        }

        //  请求正常时的上报逻辑
        if (statMerge.restDnsStat.errorCode == 0 && !statMerge.restDnsStat.cached) {
            BackupResolver backupInfo = BackupResolver.getInstance();
            //  请求成功后将ErrorCount置为0
            backupInfo.setErrorCount(0);
            //  todo: 正常解析全量上报
            //   请求成功后在spend上报次数和上报时间间隔满足的条件下进行正常路径解析时长的上报
            if (sDnsConfig.enableReport) {
                //  获取手机卡运营商code
                String carrierCode = AttaHelper.getSimOperator(context);
                //  获取当前dnsip
                String dnsIp = backupInfo.getDnsIp();
                //  a记录/ aaaa记录解析耗时上报
                if (statMerge.restDnsStat.errorCode == 0) {
                    DnsExecutors.MAIN.execute(AttaHelper.report(carrierCode, statMerge.netType, sDnsConfig.lookupExtra.bizId, sDnsConfig.channel, "HttpDnsSpend", System.currentTimeMillis(), dnsIp, BuildConfig.VERSION_NAME, AttaHelper.getSystemModel(), "Andriod", AttaHelper.getSystemVersion(), statMerge.restDnsStat.costTimeMills, statMerge.hostname, reqType, sDnsConfig.timeoutMills, statMerge.restDnsStat.ttl, statMerge.restDnsStat.errorCode, statMerge.restDnsStat.statusCode, statMerge.restDnsStat.cached, 1, CommonUtils.toStringList(statMerge.localDnsStat.ips, ReportConst.IP_SPLITTER), CommonUtils.toStringList(statMerge.restDnsStat.ips, ReportConst.IP_SPLITTER)));
                }
            }
        }

        //  灯塔反射引入
        if (!ReportManager.canReport()) {
            return;
        }

        // NOTE: 上报字段增减, 记得修改capacity
        Map<String, String> lookupMethodCalledEventMap = CollectionCompat.createMap(29);

        IpSet ipSet = lookupResult.ipSet;
        lookupMethodCalledEventMap.put(ReportConst.INET_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(ipSet.v4Ips, ReportConst.IP_SPLITTER));
        lookupMethodCalledEventMap.put(ReportConst.INET6_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(ipSet.v6Ips, ReportConst.IP_SPLITTER));

        lookupMethodCalledEventMap.put(ReportConst.CHANNEL_KEY, statMerge.channel);
        lookupMethodCalledEventMap.put(ReportConst.NETWORK_TYPE_KEY, statMerge.netType);
        lookupMethodCalledEventMap.put(ReportConst.HOSTNAME_KEY, statMerge.hostname);
        lookupMethodCalledEventMap.put(ReportConst.NETWORK_STACK_KEY,
                String.valueOf(statMerge.curNetStack));

        lookupMethodCalledEventMap.put(ReportConst.LOCAL_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(statMerge.localDnsStat.ips, ReportConst.IP_SPLITTER));
        lookupMethodCalledEventMap.put(ReportConst.LOCAL_LOOKUP_COST_TIME_MILLS_KEY,
                String.valueOf(statMerge.localDnsStat.costTimeMills));

        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_CACHE_HIT_KEY,
                String.valueOf(statMerge.restDnsStat.cached));
        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_ERROR_CODE_KEY,
                String.valueOf(statMerge.restDnsStat.errorCode));
        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_ERROR_MESSAGE_KEY,
                statMerge.restDnsStat.errorMsg);
        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(statMerge.restDnsStat.ips, ReportConst.IP_SPLITTER));
        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_TTL_KEY,
                String.valueOf(statMerge.restDnsStat.ttl));
        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_CLIENT_IP_KEY,
                statMerge.restDnsStat.clientIp);
        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_COST_TIME_MILLS_KEY,
                String.valueOf(statMerge.restDnsStat.costTimeMills));
        lookupMethodCalledEventMap.put(ReportConst.REST_LOOKUP_RETRY_TIMES_KEY,
                String.valueOf(statMerge.restDnsStat.retryTimes));

        lookupMethodCalledEventMap.put(ReportConst.LOOKUP_RESPONSE_STATUS_CODE,
                String.valueOf(statMerge.restDnsStat.statusCode));

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



    private static void reportStatisticsEvent() {
        if (!ReportManager.canReport()) {
            return;
        }
        for (Map.Entry<String, Object[]> item : statisticsMap.entrySet()) {
            Object[] temp = item.getValue();
            int errCount = (int) temp[1];
            int curCount = (int) temp[2];
            int spendAvg = (int) temp[0] / (errCount + curCount);
            StatisticsMerge stat = (StatisticsMerge) ((LookupResult) temp[3]).stat;
            //  获取手机卡运营商code
            String carrierCode = AttaHelper.getSimOperator(DnsService.getAppContext());
            //  获取当前dnsip
            String dnsIp = BackupResolver.getInstance().getDnsIp();
            if (errCount > 0) {
                // 为空的缓存统计项上报，解析结果不上报
                DnsExecutors.MAIN.execute(AttaHelper.report(carrierCode, stat.netType, sDnsConfig.lookupExtra.bizId, sDnsConfig.channel, "HttpDnsSpend", System.currentTimeMillis(), dnsIp, BuildConfig.VERSION_NAME, AttaHelper.getSystemModel(), "Andriod", AttaHelper.getSystemVersion(), spendAvg, stat.hostname, null, sDnsConfig.timeoutMills, 0, 0, 0, true, errCount, null, null));
            }
            if (curCount > 0) {
                // 有值的缓存统计项上报，解析结果不上报
                DnsExecutors.MAIN.execute(AttaHelper.report(carrierCode, stat.netType, sDnsConfig.lookupExtra.bizId, sDnsConfig.channel, "HttpDnsSpend", System.currentTimeMillis(), dnsIp, BuildConfig.VERSION_NAME, AttaHelper.getSystemModel(), "Andriod", AttaHelper.getSystemVersion(), spendAvg, stat.hostname, null, sDnsConfig.timeoutMills, 0, 0, 0, true, curCount, null, null));
            }
        }
        statisticsMap.clear();
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
