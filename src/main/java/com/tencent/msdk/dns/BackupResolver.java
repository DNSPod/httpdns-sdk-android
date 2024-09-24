package com.tencent.msdk.dns;

import static com.tencent.msdk.dns.core.ConfigFromServer.scheduleRetryRequest;

import android.content.Context;
import android.content.SharedPreferences;

import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.DebounceTask;
import com.tencent.msdk.dns.base.utils.IpValidator;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsManager;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;
import com.tencent.msdk.dns.report.ReportHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 容灾记录类，目前主要记录ErrorCount的值
 */
public class BackupResolver {
    private static BackupResolver mBackupResolver = null; //   静态对象
    private static final String PREFS_NAME = "HTTPDNSFile";
    private static final String SAVE_KEY = "httpdnsIps";
    private static final String TIMESTAMP_SUFFIX = "_timestamp";

    private BackupResolver() {
    }

    // 当前ip解析失败的次数
    private AtomicInteger mErrorCount = new AtomicInteger(0);
    // 解析失败的最大次数
    private final int maxErrorCount = 3;
    private DnsConfig mConfig;
    //  解析ips
    List<String> dnsIps;
    // 当前对应的解析ip index
    private int mIpIndex = 0;

    public static BackupResolver getInstance() {
        // 静态get方法
        if (mBackupResolver == null) {
            synchronized (BackupResolver.class) {
                if (mBackupResolver == null) {
                    mBackupResolver = new BackupResolver();
                }
            }
        }
        return mBackupResolver;
    }

    public void init(DnsConfig dnsConfig) {
        mConfig = dnsConfig;
        mErrorCount = new AtomicInteger(0);
        // http和https是两个IP列表，服务列表默认值为SDK配置中的数据
        setDnsIps(getBackUpIps());
        // 从缓存中获取动态服务列表，并设置。
        DnsExecutors.WORK.execute(new Runnable() {
            @Override
            public void run() {
                List<String> ipList = getDNSIpsFromPreference();
                if (!ipList.isEmpty()) {
                    setDnsIps(ipList);
                }
            }
        });
    }

    /**
     * DNS解析IP列表设置
     *
     * @param ips 解析IP列表
     */
    public void setDnsIps(List<String> ips) {
        if (!ips.isEmpty()) {
            dnsIps = ips;
            DnsLog.d("dns servers Ips: " + dnsIps);
            mIpIndex = 0;
            mErrorCount.set(0);
        }
    }

    public void getServerIps() {
        if (Const.HTTPS_CHANNEL.equals(mConfig.channel) || !DnsService.getDnsConfig().enableDomainServer) {
            return;
        }
        getServerIpsTask.run();
    }

    /**
     * 从SDK配置中获取当前解密方式的解析IP列表
     *
     * @return 解析IP列表
     */
    private List<String> getBackUpIps() {
        if (Const.HTTPS_CHANNEL.equals(mConfig.channel) && BuildConfig.HTTPS_DNS_SERVER.length > 0) {
            return new ArrayList<>(Arrays.asList(BuildConfig.HTTPS_DNS_SERVER));
        } else {
            return new ArrayList<>(Arrays.asList(BuildConfig.HTTP_DNS_SERVER));
        }
    }

    public void incrementErrorCount() {
        Integer count = mErrorCount.incrementAndGet();
        DnsLog.d(Thread.currentThread().getName() + " increment mErrorCount: " + count);
    }

    public void setErrorCount(Integer count) {
        mErrorCount.set(count);
        DnsLog.d(Thread.currentThread().getName() + " set mErrorCount: " + count);
    }

    public int getErrorCount() {
        return mErrorCount.get();
    }

    /**
     * 校验是否满足上报条件-当前失败次数>=解析失败的最大次数时进行上报
     *
     * @return
     */
    public boolean getCanReport(int errorCount) {
        return errorCount >= maxErrorCount;
    }

    /**
     * 主IP故障，切换备份IP策略
     * 1. 主备IP切换：在精确性、速度上折中处理，主IP解析的同时，会发起LocalDNS解析，若主IP首次解析不成功，立即返回
     * 上次解析结果，如果没有上次解析结果，则返回LocalDNS解析结果，如果主IP 3次解析不成功，则切换到备份IP进行解析。
     * 2. 备份IP切换 域名兜底：所有备份IP都经超过3次不通，切换到域名兜底解析。
     * 3. 去掉恢复逻辑。新增当轮询切回主IP时，立即获取动态IP列表
     * 4. 通过参数控制 切换策略的次数判断（默认3次）
     */
    public String getDnsIp() {
        //  mIpIndex+1 进行容灾IP切换
        if (mErrorCount.get() >= maxErrorCount) {
            // 当前的失败次数达到了切换阈值时进行ipIndex的首尾循环，这里重新切回主IP。且重新获取动态解析服务IP。
            if (mIpIndex >= dnsIps.size() - 1) {
                mIpIndex = 0;
                // 调度列表完成切回主IP时，立即调度动态IP列表
                scheduleRetryRequest(0);
            } else {
                mIpIndex++;
            }
            //  ip切换后清空ip错误次数
            mErrorCount.set(0);
            DnsLog.d("IP Changed：" + dnsIps.get(mIpIndex));
        }
        return dnsIps.get(mIpIndex);
    }

    /**
     * 服务域名解析，获取服务IP
     * https加密方式不支持域名接入
     * 国内站在初始化和网络变更时发起解析服务刷新服务IP，刷新IP不命中缓存
     * 国际站服务IP按州部署，无须在网络变更时刷新
     */
    DebounceTask getServerIpsTask = DebounceTask.build(new Runnable() {
        @Override
        public void run() {
            try {
                String dnsIp = BackupResolver.getInstance().getDnsIp();
                String domain = BuildConfig.DOMAIN_SERVICE_DOMAINS[0];
                LookupExtra lookupExtra = new LookupExtra(BuildConfig.DOMAIN_SERVICE_ID,
                        BuildConfig.DOMSIN_SERVICE_KEY, BuildConfig.DOMAIN_SERVICE_TOKEN);
                LookupParameters lookupParameters = new LookupParameters.Builder<LookupExtra>()
                        .dnsIp(dnsIp)
                        .channel("DesHttp")
                        .hostname(domain)
                        .lookupExtra(lookupExtra)
                        .context(DnsService.getContext())
                        .timeoutMills(2000)
                        .enableAsyncLookup(true)
                        .fallback2Local(false)
                        .build();
                LookupResult result = DnsManager.lookupWrapper(lookupParameters);
                ReportHelper.attaReportDomainServerLookupEvent(result);
                if (result.stat.lookupSuccess()) {
                    List<String> mergeList = new ArrayList<>();
                    List<String> serverIps = Arrays.asList(result.ipSet.v4Ips);
                    List<String> backUpIps = getBackUpIps();
                    mergeList.addAll(serverIps);
                    mergeList.addAll(backUpIps);
                    setDnsIps(mergeList);
                }
            } catch (Exception e) {
                DnsLog.w(e, "getServerIpsTask failed");
            }
        }
    }, 15L);

    /**
     * 设置动态服务IP并存储在本地
     *
     * @param ips            ip字符串列表,用;分割 eg 1.2.3.4;1.2.4.5
     * @param expirationTime 分钟，范围为1-1440 min
     */
    public void handleDynamicDNSIps(String ips, int expirationTime) {
        if (ips.isEmpty()) return;
        List<String> ipList = Arrays.asList(ips.split(";"));
        List<String> filterIpList = new ArrayList<>();
        for (String item : ipList) {
            if (IpValidator.isV4Ip(item)) {
                filterIpList.add(item);
            }
        }
        BackupResolver.getInstance().setDnsIps(filterIpList);
        SharedPreferences sharedPreferences = DnsService.getContext().getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SAVE_KEY, String.join(";", filterIpList));
        long currentTime = System.currentTimeMillis();
        editor.putLong(SAVE_KEY + TIMESTAMP_SUFFIX, currentTime + expirationTime * 60 * 1000);
        editor.apply(); // 或者使用 commit()
    }

    /**
     * 从缓存中获取动态服务IP列表
     *
     * @return 未过期的动态服务IP列表
     */
    private List<String> getDNSIpsFromPreference() {
        SharedPreferences sharedPreferences = DnsService.getContext().getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        long currentTime = System.currentTimeMillis();
        long expirationTime = sharedPreferences.getLong(SAVE_KEY + TIMESTAMP_SUFFIX, 0);
        String ips = sharedPreferences.getString(SAVE_KEY, "");
        List<String> ipsList = new ArrayList<>();

        if (!ips.isEmpty()) {
            if (currentTime > expirationTime) {
                // 数据已过期，删除数据
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(SAVE_KEY);
                editor.remove(SAVE_KEY + TIMESTAMP_SUFFIX);
                editor.apply();
            } else {
                ipsList = Arrays.asList(ips.split(";"));
            }
        }
        return ipsList;
    }
}
