package com.tencent.msdk.dns.core.rest.share.rsp;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.IpValidator;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;

import java.util.Arrays;
import java.util.Map;

public final class Response {

    public static final Response EMPTY =
            new Response(Const.INVALID_IP, Const.EMPTY_IPS, null);

    public static final Response NEED_CONTINUE =
            new Response(Const.INVALID_IP, Const.EMPTY_IPS, null);

    public final String clientIp;
    public final String[] ips;
    public final Map<String, Integer> ttl;

    Response(int family, String clientIp, String[] ips, Map<String, Integer> ttl) {
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
//        if (CommonUtils.isEmpty(ips)) {
//            throw new IllegalArgumentException("ips".concat(Const.EMPTY_TIPS));
//        }
//        if (isTtlInvalid(ttl)) {
//            throw new IllegalArgumentException("ttl".concat(Const.INVALID_TIPS));
//        }

        this.clientIp = clientIp;
        this.ips = ips;
        this.ttl = ttl;
    }

    Response(String clientIp, String[] inet4Ips, String[] inet6Ips, Map<String, Integer> ttl) {
        // NOTE: 允许unspecific
        if (TextUtils.isEmpty(clientIp)) {
            throw new IllegalArgumentException("clientIp".concat(Const.EMPTY_TIPS));
        }
        // NOTE: ips为空即为无效响应
        if (CommonUtils.isEmpty(inet4Ips) && CommonUtils.isEmpty(inet6Ips)) {
            throw new IllegalArgumentException("ips".concat(Const.EMPTY_TIPS));
        }
        // NOTE: 前面先进行一次参数校验, 再尝试处理后台返回(格式正确但)内容不符预期情况
        inet4Ips = fixIps(DnsDescription.Family.INET, inet4Ips);
        inet6Ips = fixIps(DnsDescription.Family.INET6, inet6Ips);

//        if (isTtlInvalid(ttl)) {
//            throw new IllegalArgumentException("ttl".concat(Const.INVALID_TIPS));
//        }
        int inet4IpsLength = inet4Ips.length;
        int inet6IpsLength = inet6Ips.length;

        inet4Ips = Arrays.copyOf(inet4Ips, inet4IpsLength + inet6IpsLength);
        System.arraycopy(inet6Ips, 0, inet4Ips, inet4IpsLength, inet6IpsLength);

        this.clientIp = clientIp;
        this.ips = inet4Ips;
        this.ttl = ttl;
    }

    // NOTE: 仅用于创建空实现, 空实现实际上为无效值, 无法通过常规构造器创建
    private Response(String clientIp, String[] ips, Map<String, Integer> ttl) {
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
                // 将有效IP添加到realIps中
                realIps[j--] = ip;
            }
        }
        return realIps;
    }
}
