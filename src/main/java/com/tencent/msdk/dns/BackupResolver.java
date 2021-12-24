package com.tencent.msdk.dns;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 容灾记录类，目前主要记录ErrorCount的值
 */
public class BackupResolver {
    private static BackupResolver mBackupResolver = null; //   静态对象

    private BackupResolver() {
    }

    // 当前ip解析失败的次数
    private int mErrorCount = 0;
    // 解析失败的最大次数
    private int maxErrorCount = 3;
    private DnsConfig mConfig;
    //  解析ips
    List<String> backupIps;
    // 当前对应的解析ip index
    private int mIpIndex = 0;
    // 记录主ip切换时间，每隔10min切回测试一次主IP（不主动探测主备IP是否恢复）
    private long mBackupTime = 0;
    // 尝试切回主ip的间隔时间，默认为10分钟
    private long mInterval = 10 * 60 * 1000;

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
        mErrorCount = 0;
        //  http和https是两个IP
        if (Const.HTTPS_CHANNEL.equals(dnsConfig.channel)) {
            backupIps = new ArrayList<String>(Arrays.asList(mConfig.dnsIp, "119.28.28.99"));
        } else {
            backupIps = new ArrayList<String>(Arrays.asList(mConfig.dnsIp, "119.28.28.98"));
        }

    }

    public void setErrorCount(int errorCount) {
        if (0 > errorCount) {
            throw new IllegalArgumentException("errorCount".concat(Const.LESS_THAN_0_TIPS));
        }
        this.mErrorCount = errorCount;
        DnsLog.d("ErrorInfo mErrorCount: " + mErrorCount);
    }

    public int getErrorCount() {
        return this.mErrorCount;
    }

    /**
     * 校验是否满足上报条件-当前失败次数>=解析失败的最大次数时进行上报
     * @return
     */
    public boolean getCanReport(int errorCount) {
        return errorCount>=maxErrorCount;
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
            mErrorCount = 0;
        }
        //  mIpIndex+1 进行容灾IP切换
        if (mErrorCount >= maxErrorCount) {
            // 记录主ip切走的时间
            if (mIpIndex == 0) {
                mBackupTime = SystemClock.elapsedRealtime();
            }
            // 当前的失败次数达到了切换阈值时进行ipIndex的首尾循环，这里重新切回主IP
            if (mIpIndex >= backupIps.size() - 1) {
                mIpIndex = 0;
                mBackupTime = 0;
            } else {
                mIpIndex++;
            }
            //  ip切换后清空ip错误次数
            mErrorCount = 0;
        }

        String backip = backupIps.get(mIpIndex);

        return TextUtils.isEmpty(backip) ? mConfig.dnsIp : backip;
    }
}
