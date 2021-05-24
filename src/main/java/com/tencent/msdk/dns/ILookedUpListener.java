package com.tencent.msdk.dns;

import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

/**
 * 监控SDK的解析情况的接口
 */
public interface ILookedUpListener {

    /**
     * 通过接口调用完成域名解析后的回调
     *
     * @param hostname 域名
     * @param lookupResult {@link LookupResult<StatisticsMerge>}实例, 即域名解析结果
     */
    void onLookedUp(String hostname, LookupResult<StatisticsMerge> lookupResult);

    /**
     * 预解析完成一次域名解析后的回调
     *
     * @param hostname 域名
     * @param lookupResult {@link LookupResult<StatisticsMerge>}实例, 即域名解析结果
     */
    void onPreLookedUp(String hostname, LookupResult<StatisticsMerge> lookupResult);

    /**
     * 异步解析完成一次域名解析后的回调
     *
     * @param hostname 域名
     * @param lookupResult {@link LookupResult<StatisticsMerge>}实例, 即域名解析结果
     */
    void onAsyncLookedUp(String hostname, LookupResult<StatisticsMerge> lookupResult);
}
