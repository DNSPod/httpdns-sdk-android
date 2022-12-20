package com.tencent.msdk.dns;

import android.os.SystemClock;
import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.DebounceTask;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;
import com.tencent.msdk.dns.core.DnsManager;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.deshttp.DesHttpDns;
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
    // 记录主ip切换时间，每隔10min切回测试一次主IP（不主动探测主备IP是否恢复）
    private long mBackupTime = 0;
    // 尝试切回主ip的间隔时间，默认为10分钟
    private final long mInterval = 10 * 60 * 1000;

    public static BackupResolver getInstance() {//静态get方法
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
        //  http和https是两个IP
        dnsIps = getBackUpIps();
        getServerIps();
    }

    public void getServerIps() {
        if (Const.HTTPS_CHANNEL.equals(mConfig.channel)) {
            return;
        }
        getServerIpsTask.run();
    }

    private ArrayList getBackUpIps() {
        if (Const.HTTPS_CHANNEL.equals(mConfig.channel) && !BuildConfig.HTTPS_TOLERANCE_SERVER.isEmpty()) {
            return new ArrayList<String>(Arrays.asList(mConfig.dnsIp, BuildConfig.HTTPS_TOLERANCE_SERVER));
        } else {
            return new ArrayList<String>(Arrays.asList(mConfig.dnsIp, BuildConfig.HTTP_TOLERANCE_SERVER));
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
     * 1. 主备IP切换：在精确性、速度上折中处理，主IP解析的同时，会发起LocalDNS解析，若主IP首次解析不成功，立即返回 上次解析结果，如果没有上次解析结果，则返回LocalDNS解析结果，如果主IP 3次解析不成功，则切换到备份IP进行解析。
     * <p>
     * 2. 备份IP 切换 域名兜底：所有备份IP都经超过3次不通，切换到域名兜底解析。
     * <p>
     * 3. 恢复：每隔10min切回测试一次主IP（不主动探测主备IP是否恢复）
     * <p>
     * 4. 通过参数控制 切换策略的次数判断（默认3次）、恢复主IP策略的时间间隔（默认10min）
     */
    public String getDnsIp() {
        // 当容灾ip切换超过间隔时间后尝试切换回主ip
        if ((mIpIndex != 0) && mBackupTime > 0 && ((SystemClock.elapsedRealtime() - mBackupTime) >= mInterval)) {
            mIpIndex = 0;
            mErrorCount.set(0);
        }
        //  mIpIndex+1 进行容灾IP切换
        if (mErrorCount.get() >= maxErrorCount) {
            // 记录主ip切走的时间
            if (mIpIndex == 0) {
                mBackupTime = SystemClock.elapsedRealtime();
            }
            // 当前的失败次数达到了切换阈值时进行ipIndex的首尾循环，这里重新切回主IP
            if (mIpIndex >= dnsIps.size() - 1) {
                mIpIndex = 0;
                mBackupTime = 0;
            } else {
                mIpIndex++;
            }
            //  ip切换后清空ip错误次数
            mErrorCount.set(0);
            DnsLog.d("IP Changed：" + dnsIps.get(mIpIndex));
        }
        String backip = dnsIps.get(mIpIndex);
        return TextUtils.isEmpty(backip) ? mConfig.dnsIp : backip;
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
                IDns dns = new DesHttpDns(DnsDescription.Family.INET);
                String domain = BuildConfig.INIT_SERVERS_DOMAINS[0];
                LookupExtra lookupExtra;
                if (BuildConfig.FLAVOR.equals("intl")) {
                    lookupExtra = new LookupExtra("4308", "0jXUrLWR", "");
                } else {
                    lookupExtra = new LookupExtra("34745", "Sh63l8wv", "347982594");
                }
                LookupParameters lookupParameters = new LookupParameters.Builder<LookupExtra>()
                        .dnsIp(BuildConfig.HTTP_INIT_SERVER)
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
                    dnsIps = mergeList;
                    DnsLog.d("dns servers Ips: " + dnsIps);
                    mIpIndex = 0;
                    mErrorCount.set(0);
                }
            } catch (Exception e) {
                DnsLog.w(e, "getServerIpsTask failed");
            }
        }
    }, 15L);
}
