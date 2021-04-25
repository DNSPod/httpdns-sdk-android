package com.tencent.msdk.dns.core.rest.share.rsp;

import android.text.TextUtils;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.IpValidator;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;

public final class Response {

    public static final Response EMPTY =
            new Response(Const.INVALID_IP, Const.EMPTY_IPS, Const.DEFAULT_TIME_INTERVAL);

    public static final Response NEED_CONTINUE =
        new Response(Const.INVALID_IP, Const.EMPTY_IPS, Const.DEFAULT_TIME_INTERVAL);

    public final String clientIp;
    public final String[] ips;
    public final int ttl;

    Response(int family, String clientIp, String[] ips, int ttl) {
        // NOTE: 区分inet和inet6, 不允许unspecific
        family = DnsDescription.Family.INET6 == family ? family : DnsDescription.Family.INET;
        if (TextUtils.isEmpty(clientIp)) {
            throw new IllegalArgumentException("clientIp".concat(Const.EMPTY_TIPS));
        }
        // NOTE: ips为空即为无效响应
        if (CommonUtils.isEmpty(ips)) {
            throw new IllegalArgumentException("ips".concat(Const.EMPTY_TIPS));
        }
        // NOTE: 前面先进行一次参数校验, 再尝试处理后台返回(格式正确但)内容不符预期情况
        ips = fixIps(family, ips);
        if (CommonUtils.isEmpty(ips)) {
            throw new IllegalArgumentException("ips".concat(Const.EMPTY_TIPS));
        }
        if (isTtlInvalid(ttl)) {
            throw new IllegalArgumentException("ttl".concat(Const.INVALID_TIPS));
        }

        this.clientIp = clientIp;
        if (Const.MAX_IP_COUNT >= ips.length) {
            this.ips = ips;
        } else {
            this.ips = new String[Const.MAX_IP_COUNT];
            System.arraycopy(ips, 0, this.ips, 0, Const.MAX_IP_COUNT);
        }
        this.ttl = ttl;
    }

    // NOTE: 仅用于创建空实现, 空实现实际上为无效值, 无法通过常规构造器创建
    private Response(String clientIp, String[] ips, int ttl) {
        this.clientIp = clientIp;
        this.ips = ips;
        this.ttl = ttl;
    }

    public static boolean isTtlInvalid(int ttl) {
        return 0 > ttl;
    }

    // NOTE: family区分inet和inet6, 上层保证不为unspecific; ips上层保证不为空
    private static String[] fixIps(int family, String[] ips) {
        int maxIpCount = ips.length;
        int ipCount = maxIpCount;
        for (int i = 0; i < maxIpCount; i++) {
            String ip = ips[i];
            if (DnsDescription.Family.INET6 == family) {
                if (!IpValidator.isV6Ip(ip)) {
                    ips[i] = Const.INVALID_IP;
                    ipCount--;
                }
            } else {
                if (!IpValidator.isV4Ip(ip)) {
                    ips[i] = Const.INVALID_IP;
                    ipCount--;
                }
            }
        }
        if (ipCount == maxIpCount) {
            return ips;
        }
        if (0 >= ipCount) {
            return Const.EMPTY_IPS;
        }
        String[] realIps = new String[ipCount];
        for (int i = maxIpCount - 1, j = ipCount - 1; i >= 0 && j >= 0; i--) {
            String ip = ips[i];
            if (!Const.INVALID_IP.equals(ip)) {
                realIps[j--] = ip;
            }
        }
        return realIps;
    }
}
