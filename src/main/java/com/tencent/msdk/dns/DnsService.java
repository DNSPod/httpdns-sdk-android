package com.tencent.msdk.dns;

import android.content.Context;
import android.text.TextUtils;

import com.tencent.msdk.dns.base.bugly.SharedBugly;
import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.lifecycle.ActivityLifecycleDetector;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.log.ILogNode;
import com.tencent.msdk.dns.base.network.NetworkChangeManager;
import com.tencent.msdk.dns.base.report.IReporter;
import com.tencent.msdk.dns.base.report.ReportManager;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.IpValidator;
import com.tencent.msdk.dns.core.ConfigFromServer;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;
import com.tencent.msdk.dns.core.DnsManager;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.ILookupListener;
import com.tencent.msdk.dns.core.IStatisticsMerge;
import com.tencent.msdk.dns.core.IpSet;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.cache.Cache;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;
import com.tencent.msdk.dns.report.CacheStatisticsReport;
import com.tencent.msdk.dns.report.ReportHelper;
import com.tencent.msdk.dns.report.SpendReportResolver;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * SDK对外接口类
 */
@SuppressWarnings("unused")
public final class DnsService {

    private static Context sAppContext = null;
    private static DnsConfig sConfig = null;

    private static volatile boolean sInited = false;

    public static DnsConfig getDnsConfig() {
        return sConfig;
    }

    public static Context getAppContext() {
        return sAppContext;
    }

    /**
     * 初始化SDK
     *
     * @param context <a href="https://developer.android.google.cn/reference/android/content/Context">Context</a>
     *                实例, SDK内部持有ApplicationContext用于监听网络切换等操作
     * @param config  {@link DnsConfig}实例, 用于对SDK进行相关配置
     * @throws IllegalArgumentException context为null时抛出
     */
    public static void init(Context context, /* @Nullable */DnsConfig config) {
        try {
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
            final Context appContext = context.getApplicationContext();
            sAppContext = appContext;
            sConfig = config;
            // 集成共享式bugly
            DnsExecutors.WORK.execute(new Runnable() {
                @Override
                public void run() {
                    SharedBugly.init(appContext);
                }
            });
            // 初始化解析IP服务
            BackupResolver.getInstance().init(sConfig);
            // 底层配置获取
            DnsExecutors.WORK.execute(new Runnable() {
                @Override
                public void run() {
                    ConfigFromServer.init(sConfig.lookupExtra, sConfig.channel);
                }
            });
            // 初始化SpendHelper配置为正常上报做准备
            SpendReportResolver.getInstance().init();
            NetworkChangeManager.install(appContext);
            ActivityLifecycleDetector.install(appContext);
            // Room 本地数据读取
            DnsExecutors.WORK.execute(new Runnable() {
                @Override
                public void run() {
                    Cache.getInstance().readFromDb();
                }
            });
            ReportHelper.init(config);
            DnsExecutors.sExecutorSupplier = sConfig.executorSupplier;
            setLookedUpListener(config.lookedUpListener);

            // NOTE: addReporters需保证在ReportManager init之后调用
            addReporters(config.reporters);

            sInited = true;
            preLookupAndStartAsyncLookup();
        } catch (Exception e) {
            DnsLog.w("DnsService.init failed: %s", e);
        }
    }

    /**
     * 设置UserId, 进行数据上报时区分用户, 出现问题时, 依赖该Id进行单用户问题排查
     *
     * @param userId 用户的唯一标识符, 腾讯业务建议直接使用OpenId, 腾讯云客户建议传入长度50位以内, 由字母数字下划线组合而成的字符串
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

    /**
     * 启停缓存自动刷新功能
     *
     * @param mEnablePersistentCache false：关闭，true：开启
     * @throws IllegalStateException 没有初始化时抛出
     */
    public static synchronized void enablePersistentCache(boolean mEnablePersistentCache) {
        if (!sInited) {
            throw new IllegalStateException("DnsService".concat(Const.NOT_INIT_TIPS));
        }
        sConfig.enablePersistentCache = mEnablePersistentCache;
    }

    /**
     * 设置是否使用过期缓存IP（乐观DNS）
     *
     * @param mUseExpiredIpEnable false：不使用过期（默认），true：使用过期缓存
     * @throws IllegalStateException 没有初始化时抛出
     */
    public static synchronized void setUseExpiredIpEnable(boolean mUseExpiredIpEnable) {
        if (!sInited) {
            throw new IllegalStateException("DnsService".concat(Const.NOT_INIT_TIPS));
        }
        sConfig.useExpiredIpEnable = mUseExpiredIpEnable;
    }

    /**
     * 设置是否使用本地缓存
     *
     * @param mCachedIpEnable false：不使用过期（默认），true：使用过期缓存
     * @throws IllegalStateException 没有初始化时抛出
     */
    public static synchronized void setCachedIpEnable(boolean mCachedIpEnable) {
        if (!sInited) {
            throw new IllegalStateException("DnsService".concat(Const.NOT_INIT_TIPS));
        }
        sConfig.cachedIpEnable = mCachedIpEnable;
    }

    /**
     * 设置是否上报，是否启用域名服务（获取底层配置）
     *
     * @param mEnableReport       启用日志上报
     * @param mEnableDomainServer 启用域名服务
     */
    public static void setDnsConfigFromServer(boolean mEnableReport, boolean mEnableDomainServer) {
        if (!sInited) {
            throw new IllegalStateException("DnsService".concat(Const.NOT_INIT_TIPS));
        }
        sConfig.enableReport = mEnableReport;
        sConfig.enableDomainServer = mEnableDomainServer;
    }

    /**
     * 获取DNS详情（命中缓存数据）
     *
     * @param hostname 域名，支持多个域名，用,拼接
     * @return 解析数据
     */
    public static String getDnsDetail(String hostname) {
        String dnsIp = BackupResolver.getInstance().getDnsIp();
        final LookupResult<IStatisticsMerge> lookupResult =
                DnsManager.getResultFromCache(new LookupParameters.Builder<LookupExtra>()
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

        // 收集命中缓存的数据
        DnsExecutors.WORK.execute(new Runnable() {
            @Override
            public void run() {
                CacheStatisticsReport.add(lookupResult);
            }
        });
        StatisticsMerge statMerge = (StatisticsMerge) lookupResult.stat;
        return statMerge.toJsonResult();
    }

    private static boolean enableAsyncLookup(String domain) {
        return sConfig.persistentCacheDomains != null && sConfig.persistentCacheDomains.contains(domain);
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
        DnsLog.v("DnsService.getAddrsByName(%s, %s, %b, %b) called", hostname, channel, fallback2Local,
                enableAsyncLookup);
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

    /**
     * 乐观DNS解析（批量）
     *
     * @param domain 域名
     * @return 解析结果
     * 单独接口查询情况返回：IpSet{v4Ips=[xx.xx.xx.xx], v6Ips=[xxx], ips=null}
     * 多域名批量查询返回：IpSet{v4Ips=[youtube.com:31.13.73.1, qq.com:123.151.137.18, qq.com:183.3.226.35, qq.com:61.129.7
     * .47], v6Ips=[youtube.com.:2001::42d:9141], ips=null}
     */
    public static IpSet getAddrsByNamesEnableExpired(final String domain) {
        if (!sInited) {
            throw new IllegalStateException("DnsService".concat(Const.NOT_INIT_TIPS));
        }
        String result = MSDKDnsResolver.getInstance().getDnsDetail((domain));
        IpSet ipSetReslut = IpSet.EMPTY;

        if (result.isEmpty()) {
            DnsExecutors.WORK.execute(new Runnable() {
                @Override
                public void run() {
                    // 下发解析请求
                    getAddrsByName(domain, false, true);
                }
            });
        } else {
            try {
                JSONObject temp = new JSONObject(result);
                final String requestDomain = (String) temp.get("request_name");
                // requestDomain为过期域名统计
                if (!requestDomain.isEmpty()) {
                    // 缓存过期，发起异步请求
                    DnsExecutors.WORK.execute(new Runnable() {
                        @Override
                        public void run() {
                            DnsLog.d("async look up send");
                            getAddrsByName(requestDomain, false, true);
                        }
                    });
                    // 缓存过期且不允许使用过期缓存
                    if (!DnsService.getDnsConfig().useExpiredIpEnable) {
                        return ipSetReslut;
                    }
                }
                String v4IpsStr = temp.get("v4_ips").toString();
                String v6IpsStr = temp.get("v6_ips").toString();
                String[] v4Ips = v4IpsStr.isEmpty() ? new String[0] : v4IpsStr.split(",");
                String[] v6Ips = v6IpsStr.isEmpty() ? new String[0] : v6IpsStr.split(",");
                ipSetReslut = new IpSet(v4Ips, v6Ips);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return ipSetReslut;
    }

    private static void setLookedUpListener(
            /* @Nullable */final ILookedUpListener lookedUpListener) {
        DnsLog.v("DnsService.setLookedUpListener(%s) called", lookedUpListener);

        if (null == lookedUpListener) {
            return;
        }

        DnsManager.setLookupListener(new ILookupListener() {
            @Override
            public void onLookedUp(LookupParameters lookupParameters, LookupResult<IStatisticsMerge> lookupResult) {
                String hostname = lookupParameters.hostname;
                if (!(lookupResult.stat instanceof StatisticsMerge)) {
                    DnsLog.d("Looked up for %s may be by LocalDns", hostname);
                    return;
                }
                StatisticsMerge stat = (StatisticsMerge) lookupResult.stat;
                LookupResult<StatisticsMerge> expectedLookupResult = new LookupResult<>(lookupResult.ipSet, stat);
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
        final String[] preLookupDomainsList = sConfig.preLookupDomains.toArray(new String[numOfPreLookupDomain]);
        final String preLookupDomains = CommonUtils.toStringList(preLookupDomainsList, ",");

        // 预解析调整为批量解析
        DnsExecutors.WORK.execute(new Runnable() {
            @Override
            public void run() {
                String dnsIp = BackupResolver.getInstance().getDnsIp();
                LookupResult lookupResult = DnsManager.lookupWrapper(new LookupParameters.Builder<LookupExtra>()
                        .context(sAppContext)
                        .hostname(preLookupDomains)
                        .timeoutMills(sConfig.timeoutMills)
                        .dnsIp(dnsIp)
                        .lookupExtra(sConfig.lookupExtra)
                        .channel(sConfig.channel)
                        .fallback2Local(false)
                        .blockFirst(sConfig.blockFirst)
                        .ignoreCurrentNetworkStack(true)
                        .enableAsyncLookup(true)
                        .build());
                ReportHelper.reportPreLookupEvent(lookupResult);
            }
        });
    }

    public static Context getContext() {
        return sAppContext;
    }
}
