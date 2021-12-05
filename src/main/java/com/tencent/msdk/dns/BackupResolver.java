package com.tencent.msdk.dns;

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
    private BackupResolver () {} //  私有化构造
    private int mErrorCount = 0;
//    List<String> backupHttpIps = new ArrayList<String>(Arrays.asList("119.28.28.98", "119.28.28.88"));
//    List<String> backupHttpsIps = new ArrayList<String>(Arrays.asList("119.28.28.99", "119.28.28.89"));
//    String backupHttpIp = "119.28.28.98";
//    String backupHttpsIp = "119.28.28.99";
    String backupHttpIp = "119.29.29.98";
    String backupHttpsIp = "119.29.29.99";

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

    public void setErrorCount(int errorCount){
        if (0 > errorCount) {
            throw new IllegalArgumentException("errorCount".concat(Const.LESS_THAN_0_TIPS));
        }
        this.mErrorCount = errorCount;
        DnsLog.d("ErrorInfo mErrorCount: " + mErrorCount);
    }

    public int getErrorCount(){
        return this.mErrorCount;
    }

    /**
     * 主IP故障，切换备份IP策略
     *  1. 主备IP切换：在精确性、速度上折中处理，主IP解析的同时，会发起LocalDNS解析，若主IP首次解析不成功，立即返回 上次解析结果，如果没有上次解析结果，则返回LocalDNS解析结果，如果主IP 3次解析不成功，则切换到备份IP进行解析。
     *
     *  2. 备份IP 切换 域名兜底：所有备份IP都经超过3次不通，切换到域名兜底解析。
     *
     *  3. 恢复：每隔10min切回测试一次主IP（不主动探测主备IP是否恢复）
     *
     *  4. 通过参数控制 切换策略的次数判断（默认3次）、恢复主IP策略的时间间隔（默认10min）
     */
    public String getDnsIp(String dnsip, String channel) {
        //  切换容灾IP
        if(mErrorCount>=3){
            DnsLog.d("hdns失败次数大于3，启用备份IP");
            String backip = Const.HTTPS_CHANNEL.equals(channel) ? backupHttpsIp : backupHttpIp;
            return TextUtils.isEmpty(backip) ? dnsip : backip;
        }
        return dnsip;
    }
}
