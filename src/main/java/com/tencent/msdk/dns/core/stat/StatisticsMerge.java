package com.tencent.msdk.dns.core.stat;

import android.content.Context;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.NetworkUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.IStatisticsMerge;
import com.tencent.msdk.dns.core.IpSet;
import com.tencent.msdk.dns.core.LookupContext;
import com.tencent.msdk.dns.core.local.LocalDns;
import com.tencent.msdk.dns.core.rest.share.AbsRestDns;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;
import org.json.JSONObject;

/**
 * 域名解析统计数据类
 */
public final class StatisticsMerge implements IStatisticsMerge<LookupExtra> {

    /**
     * 网络类型
     */
    public final String netType;

    /**
     * 域名
     */
    public String hostname = Const.INVALID_HOSTNAME;
    /**
     * 访问HTTPDNS服务使用的协议, UDP或者HTTP
     */
    public String channel = Const.INVALID_CHANNEL;
    /**
     * 本地网络栈
     * 0为未知网络栈
     * 1为IPv4 Only网络
     * 2为IPv6 Only网络
     * 3为双栈网络
     */
    public int curNetStack = Const.INVALID_NETWORK_STACK;

    /**
     * LocalDNS解析统计数据
     * 参见{@link LocalDns.Statistics}
     */
    public LocalDns.Statistics localDnsStat = LocalDns.Statistics.NOT_LOOKUP;
    /**
     * A记录解析统计数据
     * 参见{@link AbsRestDns.Statistics}
     */
    public AbsRestDns.Statistics restInetDnsStat = AbsRestDns.Statistics.NOT_LOOKUP;
    /**
     * AAAA记录解析统计数据
     * 参见{@link AbsRestDns.Statistics}
     */
    public AbsRestDns.Statistics restInet6DnsStat = AbsRestDns.Statistics.NOT_LOOKUP;

    /**
     * 域名解析结果IP集合
     * 参见{@link IpSet}
     */
    public IpSet ipSet;

    /**
     * 是否成功进行域名解析
     */
    public boolean lookupSuccess = false;
    /**
     * 是否得到空的解析结果
     */
    public boolean lookupFailed = true;

    private boolean hasBeenMerge = false;

    public StatisticsMerge(Context context) {
        if (null == context) {
            throw new IllegalArgumentException("context".concat(Const.NULL_POINTER_TIPS));
        }

        netType = NetworkUtils.getNetworkName(context);
    }

    @Override
    public <Statistics extends IDns.IStatistics> void merge(IDns dns, Statistics stat) {
        if (null == dns) {
            throw new IllegalArgumentException("dns".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == stat) {
            throw new IllegalArgumentException("stat".concat(Const.NULL_POINTER_TIPS));
        }
        DnsLog.v("%s.merge(%s, %s) called", super.toString(), dns, stat);
        if (!hasBeenMerge) {
            // 首次进来直接赋值
            lookupSuccess = stat.lookupSuccess();
            hasBeenMerge = true;
        } else {
            lookupSuccess = lookupSuccess && stat.lookupSuccess();
        }
        lookupFailed = lookupFailed && stat.lookupFailed();
        DnsDescription dnsDesc = dns.getDescription();
        if (Const.LOCAL_CHANNEL.equals(dnsDesc.channel)) {
            localDnsStat = (LocalDns.Statistics) stat;
        } else if (DnsDescription.Family.INET == dnsDesc.family) {
            restInetDnsStat = (AbsRestDns.Statistics) stat;
        } else if (DnsDescription.Family.INET6 == dnsDesc.family) {
            restInet6DnsStat = (AbsRestDns.Statistics) stat;
        } else {
            restInetDnsStat = (AbsRestDns.Statistics) stat;
            restInet6DnsStat = (AbsRestDns.Statistics) stat;
            // sara todo: 合并ipv4,ipv6统计处理
        }
    }

    @Override
    public void statContext(LookupContext<LookupExtra> lookupContext) {
        if (null == lookupContext) {
            throw new IllegalArgumentException("lookupContext".concat(Const.NULL_POINTER_TIPS));
        }

        hostname = lookupContext.hostname();
        channel = lookupContext.channel();
        curNetStack = lookupContext.currentNetworkStack();
    }

    @Override
    public void statResult(IpSet ipSet) {
        if (null == ipSet) {
            throw new IllegalArgumentException("ipSet".concat(Const.NULL_POINTER_TIPS));
        }

        this.ipSet = ipSet;
    }

    @Override
    public boolean lookupSuccess() {
        return lookupSuccess;
    }

    @Override
    public boolean lookupNeedRetry() {
        return (!lookupSuccess()) && (!lookupFailed());
    }

    @Override
    public boolean lookupFailed() {
        return lookupFailed;
    }

    @Override
    public String toJsonResult() {
        JSONObject jsonObject = new JSONObject();
        try {
//            jsonObject.put("v4_ips", CommonUtils.toStringList(restInetDnsStat.ips, ","));
            jsonObject.put("v4_ips", CommonUtils.toStringList(ipSet.v4Ips, ","));
            jsonObject.put("v4_ttl", String.valueOf(restInetDnsStat.ttl));
            jsonObject.put("v4_client_ip", String.valueOf(restInetDnsStat.clientIp));
//            jsonObject.put("v6_ips", CommonUtils.toStringList(restInet6DnsStat.ips, ","));
            jsonObject.put("v6_ips", CommonUtils.toStringList(ipSet.v6Ips, ","));
            jsonObject.put("v6_ttl", Const.DEFAULT_TIME_INTERVAL == restInet6DnsStat.ttl ? "" : String.valueOf(restInet6DnsStat.ttl));
            jsonObject.put("v6_client_ip", Const.INVALID_IP.equals(restInet6DnsStat.clientIp) ? "" : String.valueOf(restInet6DnsStat.clientIp));
            return jsonObject.toString();
        } catch (Exception ignore){
        }
        return "";
    }

    @Override
    public String toString() {
        return super.toString() + "{" +
                "netType='" + netType + '\'' +
                ", hostname='" + hostname + '\'' +
                ", channel='" + channel + '\'' +
                ", curNetStack=" + curNetStack +
                ", localDnsStat=" + localDnsStat +
                ", restInetDnsStat=" + restInetDnsStat +
                ", restInet6DnsStat=" + restInet6DnsStat +
                ", ipSet=" + ipSet +
                ", lookupSuccess=" + lookupSuccess +
                ", lookupGetEmptyResponse=" + lookupFailed +
                ", hasBeenMerge=" + hasBeenMerge +
                '}';
    }
}
