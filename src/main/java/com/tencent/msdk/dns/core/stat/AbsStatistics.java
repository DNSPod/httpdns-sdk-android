package com.tencent.msdk.dns.core.stat;

import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.IDns;

/**
 * 域名解析统计数据抽象基类
 */
public abstract class AbsStatistics implements IDns.IStatistics {

    /**
     * 域名解析结果
     */
    public String[] ips = Const.EMPTY_IPS;
    /**
     * 域名解析结果
     */
    public boolean isGetEmptyResponse = false;
    /**
     * 域名解析花费时间, 单位ms
     */
    public int costTimeMills = Const.DEFAULT_TIME_INTERVAL;
    /**
     * 域名解析开始时间戳
     */
    public long startLookupTimeMills = 0L;

    public void startLookup() {
        startLookupTimeMills = System.currentTimeMillis();
    }

    public void endLookup() {
        costTimeMills = (int) (System.currentTimeMillis() - startLookupTimeMills);
    }

    @Override
    public boolean lookupSuccess() {
        return Const.EMPTY_IPS != ips;
    }

    @Override
    public boolean lookupFailed() {
        return isGetEmptyResponse;
    }

    @Override
    public boolean lookupNeedRetry() {
        return (!lookupSuccess()) && (!lookupFailed());
    }
}
