package com.tencent.msdk.dns.report;

import static com.tencent.msdk.dns.base.executor.DnsExecutors.MAIN;

import android.app.Activity;

import com.tencent.msdk.dns.BackupResolver;
import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.DnsConfig;
import com.tencent.msdk.dns.base.compat.CollectionCompat;
import com.tencent.msdk.dns.base.lifecycle.ActivityLifecycleCallbacksWrapper;
import com.tencent.msdk.dns.base.lifecycle.ActivityLifecycleDetector;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.report.ReportManager;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

import java.util.Map;

public final class ReportHelper {

    private static final long REPORT_ASYNC_LOOKUP_EVENT_INTERVAL_MILLS = 5 * 60 * 1000;

    private static DnsConfig sDnsConfig;

    private static final Runnable sReportAsyncLookupEventTask = new Runnable() {
        @Override
        public void run() {
            // atta上报统计数据
            attaReportStatisticsEvent();
            MAIN.cancel(sReportAsyncLookupEventTask);
            MAIN.schedule(
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

    // 预解析调整为批量解析
    public static void reportPreLookupEvent(LookupResult lookupResult) {
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResults".concat(Const.NULL_POINTER_TIPS));
        }
        // atta上报
        attaReportLookupEvent(ReportConst.PRE_LOOKUP_EVENT_NAME, lookupResult);

        // NOTE: 上报字段增减, 记得修改capacity
        Map<String, String> preLookupEventMap = CollectionCompat.createMap(16);
        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;

        preLookupEventMap.put(ReportConst.CHANNEL_KEY, sDnsConfig.channel);
        preLookupEventMap.put(ReportConst.NETWORK_TYPE_KEY, statMerge.netType);
        preLookupEventMap.put(ReportConst.HOSTNAME_KEY, statMerge.hostname);
        preLookupEventMap.put(ReportConst.NETWORK_STACK_KEY, String.valueOf(statMerge.curNetStack));

        preLookupEventMap.put(ReportConst.REST_LOOKUP_ERROR_CODE_KEY,
                String.valueOf(statMerge.restDnsStat.errorCode));
        preLookupEventMap.put(ReportConst.REST_LOOKUP_ERROR_MESSAGE_KEY,
                statMerge.restDnsStat.errorMsg);
        preLookupEventMap.put(ReportConst.REST_LOOKUP_IPS_KEY,
                CommonUtils.toStringList(statMerge.restDnsStat.ips, ReportConst.IP_SPLITTER));
        preLookupEventMap.put(ReportConst.REST_LOOKUP_TTL_KEY,
                String.valueOf(statMerge.restDnsStat.ttl));
        preLookupEventMap.put(ReportConst.REST_LOOKUP_CLIENT_IP_KEY,
                statMerge.restDnsStat.clientIp);
        preLookupEventMap.put(ReportConst.REST_LOOKUP_COST_TIME_MILLS_KEY,
                String.valueOf(statMerge.restDnsStat.costTimeMills));
        preLookupEventMap.put(ReportConst.REST_LOOKUP_RETRY_TIMES_KEY,
                String.valueOf(statMerge.restDnsStat.retryTimes));
        preLookupEventMap.put(ReportConst.LOOKUP_RESPONSE_STATUS_CODE,
                String.valueOf(statMerge.restDnsStat.statusCode));

        addCommonConfigInfo(preLookupEventMap);

        report(ReportConst.PRE_LOOKUP_EVENT_NAME, preLookupEventMap);
    }

    public static void reportLookupMethodCalledEvent(LookupResult lookupResult) {
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }
        if (!(lookupResult.stat instanceof StatisticsMerge)) {
            DnsLog.w("lookupResult.stat is not instanceof StatisticsMerge");
            return;
        }
        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;

        // 1. 当httpdns解析完成，（非异步请求）命中缓存时，进行上报。
        // 2. 当允许使用过期缓存配置下(httponly)，httpdns异步请求完成时，进行上报。
        // 3. 当httpdns和localdns都endLookup时，即costTimeMills不等于初始值0，即为两个线程解析结束，进行上报。
        // 其他，返回不再打印
        if (statMerge.restDnsStat.cached) {
            // 命中缓存的数据，统计上报
            CacheStatisticsReport.add(lookupResult);
        } else if (sDnsConfig.useExpiredIpEnable) {
            attaReportLookupEvent(ReportConst.EXPIRED_ASYNC_LOOKUP_EVENT_NAME, lookupResult);
        } else if (statMerge.restDnsStat.costTimeMills > 0 && statMerge.localDnsStat.costTimeMills > 0) {
            attaReportLookupEvent(ReportConst.LOOKUP_METHOD_CALLED_EVENT_NAME, lookupResult);
        } else {
            return;
        }

        // NOTE: 上报字段增减, 记得修改capacity
        Map<String, String> lookupMethodCalledEventMap = CollectionCompat.createMap(20);

        lookupMethodCalledEventMap.put(ReportConst.CHANNEL_KEY, statMerge.channel);
        lookupMethodCalledEventMap.put(ReportConst.NETWORK_TYPE_KEY, statMerge.netType);
        lookupMethodCalledEventMap.put(ReportConst.HOSTNAME_KEY, statMerge.hostname);
        if (!statMerge.requestHostname.equals(statMerge.hostname)) {
            lookupMethodCalledEventMap.put(ReportConst.REQUEST_HOSTNAME_KEY, statMerge.requestHostname);
        }
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
        MAIN.schedule(
                sReportAsyncLookupEventTask, REPORT_ASYNC_LOOKUP_EVENT_INTERVAL_MILLS);
        ActivityLifecycleDetector.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksWrapper() {
            @Override
            public void onActivityStopped(Activity activity) {
                MAIN.execute(sReportAsyncLookupEventTask);
            }
        });
    }

    public static void attaReportAsyncLookupEvent(LookupResult lookupResult) {
        attaReportLookupEvent(ReportConst.ASYNC_LOOKUP_EVENT_NAME, lookupResult);
    }

    public static void attaReportDomainServerLookupEvent(LookupResult lookupResult) {
        attaReportLookupEvent(ReportConst.DOMAIN_SERVER_LOOKUP_EVENT_NAME, lookupResult);
    }

    private static void attaReportLookupEvent(String eventName, LookupResult lookupResult) {
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }
        if (!(lookupResult.stat instanceof StatisticsMerge)) {
            DnsLog.w("lookupResult.stat is not instanceof StatisticsMerge");
            return;
        }

        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;

        BackupResolver backupInfo = BackupResolver.getInstance();
        //  获取当前dnsip
        String dnsIp = backupInfo.getDnsIp();
        String reqType = AttaHelper.getReqType(statMerge.curNetStack);
        Boolean enableReport = sDnsConfig.enableReport;

        if (!statMerge.restDnsStat.cached) {
            //  请求正常时的上报逻辑全量上报
            if (statMerge.restDnsStat.errorCode == 0) {
                //  请求成功后将ErrorCount置为0
                backupInfo.setErrorCount(0);
                if(enableReport) {
                    MAIN.execute(AttaHelper.report(statMerge.netType, sDnsConfig.lookupExtra.bizId,
                            sDnsConfig.appId, sDnsConfig.channel, eventName, System.currentTimeMillis(), dnsIp,
                            statMerge.restDnsStat.costTimeMills, statMerge.localDnsStat.costTimeMills,
                            statMerge.requestHostname, reqType, sDnsConfig.timeoutMills, statMerge.restDnsStat.ttl,
                            statMerge.restDnsStat.errorCode, statMerge.restDnsStat.statusCode,
                            statMerge.restDnsStat.cached, 1,
                            CommonUtils.toStringList(statMerge.localDnsStat.ips, ReportConst.IP_SPLITTER),
                            CommonUtils.toStringList(statMerge.restDnsStat.ips, ReportConst.IP_SPLITTER)));
                }
            } else {
                //  ErrorCode==2 (超时)进行容灾处理，https请求存在请求异常超时时间>timeoutMills,此时errCode为1
                if (statMerge.restDnsStat.errorCode == 2
                        || (Const.HTTPS_CHANNEL.equals(sDnsConfig.channel) && (statMerge.restDnsStat.errorCode == 1))) {
                    // 解析失败，仅当达到最大失败次数满足切换IP时候上报
                    if (enableReport && backupInfo.getCanReport(backupInfo.getErrorCount() + 1)) {
                        MAIN.execute(AttaHelper.report(statMerge.netType, sDnsConfig.lookupExtra.bizId,
                                sDnsConfig.appId, sDnsConfig.channel, eventName, System.currentTimeMillis(), dnsIp,
                                statMerge.restDnsStat.costTimeMills, statMerge.localDnsStat.costTimeMills,
                                statMerge.requestHostname, reqType, sDnsConfig.timeoutMills,
                                statMerge.restDnsStat.ttl, statMerge.restDnsStat.errorCode,
                                statMerge.restDnsStat.statusCode, statMerge.restDnsStat.cached, 1,
                                CommonUtils.toStringList(statMerge.localDnsStat.ips, ReportConst.IP_SPLITTER),
                                CommonUtils.toStringList(statMerge.restDnsStat.ips, ReportConst.IP_SPLITTER)));
                    }
                    // 报错记录+1
                    backupInfo.incrementErrorCount();
                    DnsLog.d("dnsip连接失败, 当前失败次数：" + backupInfo.getErrorCount());
                } else {
                    if (enableReport) {
                        MAIN.execute(AttaHelper.report(statMerge.netType, sDnsConfig.lookupExtra.bizId,
                                sDnsConfig.appId, sDnsConfig.channel, eventName, System.currentTimeMillis(), dnsIp,
                                statMerge.restDnsStat.costTimeMills, statMerge.localDnsStat.costTimeMills,
                                statMerge.requestHostname, reqType, sDnsConfig.timeoutMills, statMerge.restDnsStat.ttl,
                                statMerge.restDnsStat.errorCode, statMerge.restDnsStat.statusCode,
                                statMerge.restDnsStat.cached, 1,
                                CommonUtils.toStringList(statMerge.localDnsStat.ips, ReportConst.IP_SPLITTER),
                                CommonUtils.toStringList(statMerge.restDnsStat.ips, ReportConst.IP_SPLITTER)));
                    }
                }
            }
        }
    }


    private static void attaReportStatisticsEvent() {
        Map<String, Object[]> cacheStatisticsMap = CacheStatisticsReport.offerAll();
        if (!sDnsConfig.enableReport || cacheStatisticsMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object[]> item : cacheStatisticsMap.entrySet()) {
            Object[] temp = item.getValue();
            int errCount = (int) temp[1];
            int curCount = (int) temp[2];
            int spendAvg = (int) temp[0] / (errCount + curCount);
            //  获取当前dnsip
            String dnsIp = BackupResolver.getInstance().getDnsIp();
            if (errCount > 0) {
                // 为空的缓存统计项上报，解析结果不上报
                MAIN.execute(AttaHelper.report("", sDnsConfig.lookupExtra.bizId, sDnsConfig.appId,
                        sDnsConfig.channel, ReportConst.LOOKUP_FROM_CACHED_EVENT_NAME, System.currentTimeMillis(),
                        dnsIp, spendAvg, 0, item.getKey(), "", sDnsConfig.timeoutMills, null, 3, 0, true, errCount,
                        null, null));
            }
            if (curCount > 0) {
                // 有值的缓存统计项上报，解析结果不上报
                MAIN.execute(AttaHelper.report("", sDnsConfig.lookupExtra.bizId, sDnsConfig.appId,
                        sDnsConfig.channel, ReportConst.LOOKUP_FROM_CACHED_EVENT_NAME, System.currentTimeMillis(),
                        dnsIp, spendAvg, 0, item.getKey(), "", sDnsConfig.timeoutMills, null, 0, 0, true, curCount,
                        null, null));
            }
        }
    }

    private static void addCommonConfigInfo(Map<String, String> eventMap) {
        eventMap.put(ReportConst.SDK_VERSION_KEY, BuildConfig.VERSION_NAME);
        eventMap.put(ReportConst.APP_ID_KEY, sDnsConfig.appId);
        eventMap.put(ReportConst.BIZ_ID_KEY, sDnsConfig.lookupExtra.bizId);
//        eventMap.put(ReportConst.USER_ID_KEY, sDnsConfig.userId);
    }

    private static void report(String eventName, Map<String, String> eventMap) {
        ReportManager.report(ReportManager.Environment.TEST | ReportManager.Environment.RELEASE,
                eventName, eventMap);
    }
}
