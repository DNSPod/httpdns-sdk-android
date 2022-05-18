package com.tencent.msdk.dns;

import android.content.Context;
import android.text.TextUtils;

import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.lifecycle.ActivityLifecycleDetector;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.log.ILogNode;
import com.tencent.msdk.dns.base.network.NetworkChangeManager;
import com.tencent.msdk.dns.base.report.BeaconReporterInitParameters;
import com.tencent.msdk.dns.base.report.IReporter;
import com.tencent.msdk.dns.base.report.ReportManager;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.IpValidator;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;
import com.tencent.msdk.dns.core.DnsManager;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.ILookupListener;
import com.tencent.msdk.dns.core.IStatisticsMerge;
import com.tencent.msdk.dns.core.IpSet;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;
import com.tencent.msdk.dns.report.ReportHelper;
import com.tencent.msdk.dns.report.SpendReportResolver;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * SDK对外接口类
 */
@SuppressWarnings("unused")
public final class DnsService {

    private static Context sAppContext = null;
    private static DnsConfig sConfig = null;

    private static volatile boolean sInited = false;

    /**
     * 初始化SDK
     *
     * @param context <a href="https://developer.android.google.cn/reference/android/content/Context">Context</a>实例, SDK内部持有ApplicationContext用于监听网络切换等操作
     * @param config  {@link DnsConfig}实例, 用于对SDK进行相关配置
     * @throws IllegalArgumentException context为null时抛出
     */
    public static void init(Context context, /* @Nullable */DnsConfig config) {
        // NOTE: 参数检查不封装为通用方法, 是为了避免不必要的concat执行
        if (null == context) {
            throw new IllegalArgumentException("context".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == config) {
            config = new DnsConfig.Builder().build();
        }

        // NOTE: 在开始打日志之前设置日志开关
        DnsLog.setLogLevel(config.logLevel);
        addLogNodes(config.logNodes);
        DnsLog.v("DnsService.init(%s, %s) called, ver:%s", context, config, BuildConfig.VERSION_NAME);
        Context appContext = context.getApplicationContext();
        sAppContext = appContext;
        sConfig = config;
        // 初始化Backup配置为容灾做准备
        BackupResolver.getInstance().init(sConfig);
        // 初始化SpendHelper配置为正常上报做准备
        SpendReportResolver.getInstance().init();
        NetworkChangeManager.install(appContext);
        ActivityLifecycleDetector.install(appContext);
        // NOTE: 当前版本暂时不会提供为OneSdk版本, 默认使用灯塔上报
        ReportManager.init(ReportManager.Channel.BEACON);
        if (config.initBuiltInReporters) {
            ReportManager.initBuiltInReporter(
                    ReportManager.Channel.BEACON,
                    new BeaconReporterInitParameters(appContext, sConfig.appId));
        }
        ReportHelper.init(config);
        DnsExecutors.sExecutorSupplier = sConfig.executorSupplier;
        setLookedUpListener(config.lookedUpListener);

        // NOTE: addReporters需保证在ReportManager init之后调用
        addReporters(config.reporters);

        sInited = true;

        preLookupAndStartAsyncLookup();
    }

    /**
     * 设置UserId, 进行数据上报时区分用户, 出现问题时, 依赖该Id进行单用户问题排查
     *
     * @param userId 用户的唯一标识符, 腾讯业务建议直接使用OpenId, 腾讯云客户建议传入长度50位以内, 由字母数字下划线组合而成的字符串
     * @return 是否设置成功, true为设置成功, false为设置失败
     * @throws IllegalStateException    没有初始化时抛出
     * @throws IllegalArgumentException userId为空时抛出
     */
    public static synchronized void setUserId(String userId) {
        if (!sInited) {
            throw new IllegalStateException("DnsService".concat(Const.NOT_INIT_TIPS));
        }
        if (TextUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId".concat(Const.EMPTY_TIPS));
        }
        sConfig.userId = userId;
    }

    public static String getDnsDetail(String hostname) {
        String dnsIp = BackupResolver.getInstance().getDnsIp();
        LookupResult<IStatisticsMerge> lookupResult = DnsManager.getResultFromCache(new LookupParameters.Builder<LookupExtra>()
                .context(sAppContext)
                .hostname(hostname)
                .timeoutMills(sConfig.timeoutMills)
                .dnsIp(dnsIp)
                .lookupExtra(sConfig.lookupExtra)
                .channel(sConfig.channel)
                .fallback2Local(true)
                .blockFirst(sConfig.blockFirst)
                .enableAsyncLookup(false)
                .customNetStack(sConfig.customNetStack)
                .build());
        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;
        return statMerge.toJsonResult();
    }

    /**
     * 进行域名解析
     *
     * @param hostname 域名
     * @return {@link IpSet}实例, 即解析得到的Ip集合
     * @throws IllegalStateException 没有初始化时抛出
     */
    public static IpSet getAddrsByName(/* @Nullable */String hostname) {
        return getAddrsByName(hostname, sConfig.channel, true, false);
    }

    /**
     * 进行域名解析
     *
     * @param hostname       域名
     * @param fallback2Local 访问HTTPDNS服务失败时, 是否fallback到LocalDNS进行域名解析
     * @return {@link IpSet}实例, 即解析得到的Ip集合
     * @throws IllegalStateException 没有初始化时抛出
     */
    public static IpSet getAddrsByName(
            /* @Nullable */String hostname, boolean fallback2Local) {
        return getAddrsByName(hostname, sConfig.channel, fallback2Local, false);
    }

    /**
     * ***** 实验性功能 *****
     * 进行域名解析
     *
     * @param hostname          域名
     * @param fallback2Local    访问HTTPDNS服务失败时, 是否fallback到LocalDNS进行域名解析
     * @param enableAsyncLookup 是否对当前域名启用异步解析
     * @return {@link IpSet}实例, 即解析得到的Ip集合
     * @throws IllegalStateException 没有初始化时抛出
     */
    private static IpSet getAddrsByName(
            /* @Nullable */String hostname, boolean fallback2Local, boolean enableAsyncLookup) {
        return getAddrsByName(hostname, sConfig.channel, fallback2Local, enableAsyncLookup);
    }

    @SuppressWarnings("SameParameterValue")
    private static IpSet getAddrsByName(/* @Nullable */String hostname,
            /* @Nullable */String channel, boolean fallback2Local, boolean enableAsyncLookup) {
        if (!sInited) {
            throw new IllegalStateException("DnsService".concat(Const.NOT_INIT_TIPS));
        }
        if (TextUtils.isEmpty(hostname) || TextUtils.isEmpty(hostname = hostname.trim())) {
            DnsLog.d("Hostname is empty");
            return IpSet.EMPTY;
        }
        if (IpValidator.isV4Ip(hostname)) {
            DnsLog.d("Hostname %s is an v4 ip, just return it", hostname);
            return new IpSet(new String[]{hostname}, Const.EMPTY_IPS);
        }
        if (IpValidator.isV6Ip(hostname)) {
            DnsLog.d("Hostname %s is an v6 ip, just return it", hostname);
            return new IpSet(Const.EMPTY_IPS, new String[]{hostname});
        }
        if (TextUtils.isEmpty(channel)) {
            channel = sConfig.channel;
        }
        //  进行容灾判断是否要切换备份域名
        String dnsIp = BackupResolver.getInstance().getDnsIp();

        // NOTE: trim操作太重，下层默认不再处理
        DnsLog.v("DnsService.getAddrsByName(%s, %s, %b, %b) called", hostname, channel, fallback2Local, enableAsyncLookup);
        if (sConfig.needProtect(hostname)) {
            LookupResult lookupResult = DnsManager
                    .lookupWrapper(new LookupParameters.Builder<LookupExtra>()
                            .context(sAppContext)
                            .hostname(hostname)
                            .timeoutMills(sConfig.timeoutMills)
                            .dnsIp(dnsIp)
                            .lookupExtra(sConfig.lookupExtra)
                            .channel(channel)
                            .fallback2Local(fallback2Local)
                            .blockFirst(sConfig.blockFirst)
                            .enableAsyncLookup(enableAsyncLookup)
                            .customNetStack(sConfig.customNetStack)
                            .build());
            ReportHelper.reportLookupMethodCalledEvent(lookupResult, sAppContext);
            return lookupResult.ipSet;
        }
        if (fallback2Local) {
            DnsLog.d("Hostname %s is not in protected domain list, just lookup by LocalDns", hostname);
            return DnsManager
                    .lookupWrapper(new LookupParameters.Builder<>()
                            .context(sAppContext)
                            .hostname(hostname)
                            .timeoutMills(sConfig.timeoutMills)
                            .dnsIp(dnsIp)
                            .lookupExtra(IDns.ILookupExtra.EMPTY)
                            .channel(Const.LOCAL_CHANNEL)
                            .fallback2Local(false)
                            .blockFirst(sConfig.blockFirst)
                            .build())
                    .ipSet;
        }
        return IpSet.EMPTY;
    }

    private static void setLookedUpListener(
            /* @Nullable */final ILookedUpListener lookedUpListener) {
        DnsLog.v("DnsService.setLookedUpListener(%s) called", lookedUpListener);

        if (null == lookedUpListener) {
            return;
        }

        DnsManager.setLookupListener(new ILookupListener() {
            @Override
            public void onLookedUp(LookupParameters lookupParameters,
                                   LookupResult<IStatisticsMerge> lookupResult) {
                String hostname = lookupParameters.hostname;
                if (!(lookupResult.stat instanceof StatisticsMerge)) {
                    DnsLog.d("Looked up for %s may be by LocalDns", hostname);
                    return;
                }
                StatisticsMerge stat = (StatisticsMerge) lookupResult.stat;
                LookupResult<StatisticsMerge> expectedLookupResult =
                        new LookupResult<>(lookupResult.ipSet, stat);
                if (lookupParameters.ignoreCurNetStack) {
                    if (DnsDescription.Family.UN_SPECIFIC == lookupParameters.family) {
                        lookedUpListener.onPreLookedUp(hostname, expectedLookupResult);
                    } else {
                        lookedUpListener.onAsyncLookedUp(hostname, expectedLookupResult);
                    }
                } else {
                    lookedUpListener.onLookedUp(hostname, expectedLookupResult);
                }
            }
        });
    }

    private static void addLogNodes(/* @Nullable */List<ILogNode> logNodes) {
        DnsLog.v("DnsService.addLogNodes(%s) called", CommonUtils.toString(logNodes));

        if (null == logNodes) {
            return;
        }

        for (ILogNode logNode : logNodes) {
            DnsLog.addLogNode(logNode);
        }
    }

    private static void addReporters(/* @Nullable */List<IReporter> reporters) {
        DnsLog.v("DnsService.addReporters(%s) called", CommonUtils.toString(reporters));

        if (null == reporters) {
            return;
        }

        for (IReporter reporter : reporters) {
            ReportManager.addReporter(reporter);
        }
    }

    private static void preLookupAndStartAsyncLookup() {
        if (CommonUtils.isEmpty(sConfig.preLookupDomains)) {
            return;
        }

        final int numOfPreLookupDomain = sConfig.preLookupDomains.size();
        final String[] preLookupDomains =
                sConfig.preLookupDomains.toArray(new String[numOfPreLookupDomain]);
        final Set<String> asyncLookupDomains = null != sConfig.asyncLookupDomains ?
                sConfig.asyncLookupDomains : Collections.<String>emptySet();

        final LookupResult[] preLookupResults = new LookupResult[numOfPreLookupDomain];
        final CountDownLatch preLookupCountDownLatch = new CountDownLatch(numOfPreLookupDomain);
        for (int i = 0; i < numOfPreLookupDomain; i++) {
            // config保证domain不为空
            final String domain = preLookupDomains[i];
            final int iSnapshot = i;
            DnsExecutors.WORK.execute(new Runnable() {
                @Override
                public void run() {
                    String dnsIp = BackupResolver.getInstance().getDnsIp();
                    LookupParameters<LookupExtra> lookupParams =
                            new LookupParameters.Builder<LookupExtra>()
                                    .context(sAppContext)
                                    .hostname(domain)
                                    .timeoutMills(sConfig.timeoutMills)
                                    .dnsIp(dnsIp)
                                    .lookupExtra(sConfig.lookupExtra)
                                    .channel(sConfig.channel)
                                    .fallback2Local(false)
                                    .blockFirst(sConfig.blockFirst)
                                    .ignoreCurrentNetworkStack(true)
                                    .enableAsyncLookup(sConfig.asyncLookupDomains != null && sConfig.asyncLookupDomains.contains(domain))
                                    .build();
                    preLookupResults[iSnapshot] = DnsManager.lookupWrapper(lookupParams);
                    preLookupCountDownLatch.countDown();
                }
            });
        }
// TODO: 目前上报效率比较低，等预解析逻辑更新为批量后再恢复上报
//        DnsExecutors.WORK.execute(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    preLookupCountDownLatch.await();
//                    DnsLog.d("Await for pre lookup count down success");
//                } catch (Exception e) {
//                    DnsLog.w(e, "Await for pre lookup count down failed");
//                }
//                ReportHelper.reportPreLookupEvent(preLookupResults);
//            }
//        });
    }
}
