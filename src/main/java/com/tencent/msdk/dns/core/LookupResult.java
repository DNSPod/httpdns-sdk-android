package com.tencent.msdk.dns.core;

import com.google.gson.Gson;

/**
 * 域名解析结果类
 *
 * @param <Statistics> 域名解析的统计数据
 */
public final class LookupResult<Statistics extends IDns.IStatistics> {

    /**
     * 域名解析结果IP集合
     * 参见{@link IpSet}
     */
    public final IpSet ipSet;
    /**
     * 域名解析的统计数据
     */
    public final Statistics stat;

    public LookupResult(String[] ips, Statistics stat) {
        if (null == ips) {
            throw new IllegalArgumentException("ips".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == stat) {
            throw new IllegalArgumentException("stat".concat(Const.NULL_POINTER_TIPS));
        }

        this.ipSet = new IpSet(ips);
        this.stat = stat;
    }

    public LookupResult(IpSet ipSet, Statistics stat) {
        if (null == ipSet) {
            throw new IllegalArgumentException("ipSet".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == stat) {
            throw new IllegalArgumentException("stat".concat(Const.NULL_POINTER_TIPS));
        }

        this.ipSet = ipSet;
        this.stat = stat;
    }

    @Override
    public String toString() {
        return "LookupResult{" +
                "ipSet=" + ipSet +
                ", stat=" + stat +
                '}';
    }

    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
