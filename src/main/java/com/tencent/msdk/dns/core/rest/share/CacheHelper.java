package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.BackupResolver;
import com.tencent.msdk.dns.base.compat.CollectionCompat;
import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.network.IOnNetworkChangeListener;
import com.tencent.msdk.dns.base.network.NetworkChangeManager;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsManager;
import com.tencent.msdk.dns.core.ICache;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.ipRank.IpRankCallback;
import com.tencent.msdk.dns.core.ipRank.IpRankHelper;
import com.tencent.msdk.dns.core.rest.share.rsp.Response;
import com.tencent.msdk.dns.report.ReportHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public final class CacheHelper {

    private static final float ASYNC_LOOKUP_FACTOR = 0.75f;

    private final List<Runnable> mPendingTasks = new Vector<>();
    // NOTE: 先后发起两个域名解析请求, 如果后一个无法命中前一个的缓存, 则两个请求的结果都会写入缓存
    // 后一次写入应该负责清理前一次缓存写入对应的pending任务
    private final Map<String, PendingTasks> mHostnamePendingTasksMap = new ConcurrentHashMap<>();
    // pending的异步解析任务对应的参数
    private final Set<LookupParameters<LookupExtra>> mAsyncLookupParamsSet =
            Collections.synchronizedSet(
                    CollectionCompat.<LookupParameters<LookupExtra>>createSet());

    private final IDns<LookupExtra> mDns;
    private final ICache mCache;
    private final IpRankHelper mIpRankHelper = new IpRankHelper();

    CacheHelper(IDns<LookupExtra> dns, ICache cache) {
        if (null == dns) {
            throw new IllegalArgumentException("dns".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == cache) {
            throw new IllegalArgumentException("cache".concat(Const.NULL_POINTER_TIPS));
        }

        mDns = dns;
        mCache = cache;
        listenNetworkChange();
    }

    public LookupResult get(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        return mCache.get(hostname);
    }

    public void update(String hostname, LookupResult lookupResult) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }
        mCache.delete(hostname);
        mCache.add(hostname, lookupResult);
    }

    public void put(LookupParameters<LookupExtra> lookupParams, Response rsp) {
        if (null == lookupParams) {
            throw new IllegalArgumentException("lookupParams".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == rsp) {
            throw new IllegalArgumentException("rsp".concat(Const.NULL_POINTER_TIPS));
        }

        if (Response.EMPTY == rsp) {
            return;
        }

        final String[] hostnameArr = lookupParams.hostname.split(",");
        Map<String, List<String>> ipsWithHostname = new HashMap<>();
        if (hostnameArr.length > 1) {
            // 对批量域名返回值做处理
            for (String ips : rsp.ips) {
                final String[] arr = ips.split(":", 2);
                if (!ipsWithHostname.containsKey(arr[0])) {
                    ipsWithHostname.put(arr[0], new ArrayList<String>());
                }
                ipsWithHostname.get(arr[0]).add(arr[1]);
            }
        } else {
            ipsWithHostname.put(hostnameArr[0], Arrays.asList(rsp.ips));
        }


        for (final String hostname : hostnameArr) {
            List<String> ipsList= ipsWithHostname.get(hostname);
            if (ipsList != null) {
                String[] ips = ipsList.toArray(new String[0]);
                AbsRestDns.Statistics stat = new AbsRestDns.Statistics(ips, rsp.clientIp, rsp.ttl);
                stat.errorCode = ErrorCode.SUCCESS;
                mCache.add(hostname, new LookupResult<>(ips, stat));
                cacheUpdateTask(lookupParams, rsp, hostname);

                // 发起IP优选服务
                mIpRankHelper.ipv4Rank(hostname, ips, new IpRankCallback() {
                    @Override
                    public void onResult(String hostname, String[] sortedIps) {
                        LookupResult cacheResult = get(hostname);
                        // 根据排序的ip结果来对缓存结果排序
                        if (cacheResult != null) {
                            LookupResult sortedResult = mIpRankHelper.sortResultByIps(sortedIps, cacheResult);
                            update(hostname, sortedResult);
                        }
                    }
                });
            }
        }

        if (hostnameArr.length > 1) {
            // todo:批量域名的存储逻辑仍先保留。批量域名解析查询缓存仍以整个hostname为索引。
            AbsRestDns.Statistics stat = new AbsRestDns.Statistics(rsp.ips, rsp.clientIp, rsp.ttl);
            stat.errorCode = ErrorCode.SUCCESS;
            mCache.add(lookupParams.hostname, new LookupResult<>(rsp.ips, stat));
            cacheUpdateTask(lookupParams, rsp, lookupParams.hostname);
        }
    }

    private void cacheUpdateTask(LookupParameters<LookupExtra> lookupParams, Response rsp, final String hostname) {
        PendingTasks pendingTasks = mHostnamePendingTasksMap.get(hostname);
        if (null != pendingTasks) {
            if (null != pendingTasks.removeExpiredCacheTask) {
                mPendingTasks.remove(pendingTasks.removeExpiredCacheTask);
                DnsExecutors.MAIN.cancel(pendingTasks.removeExpiredCacheTask);
                pendingTasks.removeExpiredCacheTask = null;
            }
            if (null != pendingTasks.asyncLookupTask) {
                mPendingTasks.remove(pendingTasks.asyncLookupTask);
                DnsExecutors.MAIN.cancel(pendingTasks.asyncLookupTask);
                pendingTasks.asyncLookupTask = null;
            }
        } else {
            pendingTasks = new PendingTasks();
        }

        final Set<String> persistentCacheDomains = DnsService.getDnsConfig().persistentCacheDomains;
        final boolean enablePersistentCache = DnsService.getDnsConfig().enablePersistentCache;
        // 创建缓存更新任务
        if (enablePersistentCache && persistentCacheDomains != null && persistentCacheDomains.contains(hostname)) {
            final int lookupFamily = mDns.getDescription().family;
            final LookupParameters<LookupExtra> newLookupParams;
            newLookupParams =
                    new LookupParameters.Builder<>(lookupParams)
                            .hostname(hostname)
                            .enableAsyncLookup(true)
                            .fallback2Local(false)
                            .family(lookupFamily)
                            .networkChangeLookup(false)
                            .build();
            mAsyncLookupParamsSet.add(newLookupParams);
            final Runnable asyncLookupTask = new Runnable() {
                @Override
                public void run() {
                    DnsLog.d("%.2f of TTL goes by, lookup for %s(%d) async",
                            ASYNC_LOOKUP_FACTOR, hostname, lookupFamily);
                    DnsExecutors.WORK.execute(new Runnable() {
                        @Override
                        public void run() {
                            LookupResult lookupResult = DnsManager.lookupWrapper(newLookupParams);
                            AsyncLookupResultQueue.enqueue(lookupResult);
                            // atta上报
                            ReportHelper.attaReportAsyncLookupEvent(lookupResult);
                        }
                    });
                    mPendingTasks.remove(this);
                }
            };
            pendingTasks.asyncLookupTask = asyncLookupTask;
            mPendingTasks.add(asyncLookupTask);
            DnsExecutors.MAIN.schedule(
                    asyncLookupTask, (long) (ASYNC_LOOKUP_FACTOR * rsp.ttl * 1000));
        } else {
            final boolean useExpiredIpEnable = DnsService.getDnsConfig().useExpiredIpEnable;
            // 允许使用过期缓存，不下发清空缓存任务
            if (!useExpiredIpEnable) {
                final Runnable removeExpiredCacheTask = new Runnable() {
                    @Override
                    public void run() {
                        DnsLog.d("Cache of %s(%d) expired", hostname, mDns.getDescription().family);
                        mCache.delete(hostname);
                        mPendingTasks.remove(this);
                    }
                };
                pendingTasks.removeExpiredCacheTask = removeExpiredCacheTask;
                mPendingTasks.add(removeExpiredCacheTask);
                DnsExecutors.MAIN.schedule(removeExpiredCacheTask, (long) (rsp.ttl * 1000));
            }
        }

        if (!mHostnamePendingTasksMap.containsKey(hostname)) {
            mHostnamePendingTasksMap.put(hostname, pendingTasks);
        }
    }

    private void listenNetworkChange() {
        NetworkChangeManager.addNetworkChangeListener(
                new IOnNetworkChangeListener() {
                    @Override
                    public void onNetworkChange() {
                        final boolean useExpiredIpEnable = DnsService.getDnsConfig().useExpiredIpEnable;
                        // 允许使用过期缓存，不清除缓存
                        if (!useExpiredIpEnable) {
                            DnsLog.d("Network changed, clear caches");
                            mCache.clear();
                        }
                        synchronized (mPendingTasks) {
                            for (Runnable task : mPendingTasks) {
                                DnsExecutors.MAIN.cancel(task);
                            }
                        }

                        DnsLog.d("Network changed, refetch ThreeNets Ips");
                        BackupResolver.getInstance().getThreeNets();

                        // 开启自动刷新缓存后，切换网络，刷新配置域名的缓存
                        final boolean enablePersistentCache = DnsService.getDnsConfig().enablePersistentCache;
                        if (enablePersistentCache) {
                            synchronized (mAsyncLookupParamsSet) {
                                DnsLog.d("Network changed, enable persistent async lookup");
                                Iterator<LookupParameters<LookupExtra>> asyncLookupParamsIterator =
                                        mAsyncLookupParamsSet.iterator();
                                while (asyncLookupParamsIterator.hasNext()) {
                                    LookupParameters<LookupExtra> asyncLookupParams =
                                            asyncLookupParamsIterator.next();
                                    DnsLog.d("Async lookup for %s start",
                                            asyncLookupParams.hostname);
                                    final LookupParameters<LookupExtra> newLookupParams =
                                            new LookupParameters.Builder<>(asyncLookupParams)
                                                    .networkChangeLookup(true)
                                                    .build();
                                    DnsExecutors.WORK.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            LookupResult lookupResult = DnsManager.lookupWrapper(newLookupParams);
                                            AsyncLookupResultQueue.enqueue(
                                                    lookupResult);
                                            // atta上报
                                            ReportHelper.attaReportAsyncLookupEvent(lookupResult);
                                        }
                                    });
                                    asyncLookupParamsIterator.remove();
                                }
                            }
                        }
                    }
                });
    }

    private static class PendingTasks {

        public Runnable removeExpiredCacheTask;
        public Runnable asyncLookupTask;
    }
}
