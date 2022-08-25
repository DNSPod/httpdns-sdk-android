package com.tencent.msdk.dns.core;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 域名解析结果类
 */
public class IpSet implements Serializable {

    public static final IpSet EMPTY = new IpSet(Const.EMPTY_IPS, Const.EMPTY_IPS);

    /**
     * 解析得到的IPv4集合, 可能为null
     */
    /* @Nullable */ public final String[] v4Ips;
    /**
     * 解析得到的IPv6集合, 可能为null
     */
    /* @Nullable */ public final String[] v6Ips;

    /**
     * 解析得到的IP集合, 可能为null
     * 当前域名解析结果区分IPv4和IPv6, 固定为null
     */
    // NOTE: ips用于SDK支持类似Happy Eyeballs V2之类的IP排序功能使用
    /* @Nullable */ public final String[] ips;

    public IpSet(String[] v4Ips, String[] v6Ips) {
        if (null == v4Ips) {
            throw new IllegalArgumentException("v4Ips".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == v6Ips) {
            throw new IllegalArgumentException("v6Ips".concat(Const.NULL_POINTER_TIPS));
        }

        this.v4Ips = v4Ips;
        this.v6Ips = v6Ips;
        this.ips = null;
    }

    public IpSet(String[] ips) {
        if (null == ips) {
            throw new IllegalArgumentException("ips".concat(Const.NULL_POINTER_TIPS));
        }

        this.ips = ips;
        this.v4Ips = null;
        this.v6Ips = null;
    }

    @Override
    public String toString() {
        return "IpSet{" +
                "v4Ips=" + Arrays.toString(v4Ips) +
                ", v6Ips=" + Arrays.toString(v6Ips) +
                ", ips=" + Arrays.toString(ips) +
                '}';
    }
}
