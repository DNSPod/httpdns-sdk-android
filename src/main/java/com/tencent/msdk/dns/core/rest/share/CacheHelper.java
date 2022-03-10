package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

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
import com.tencent.msdk.dns.core.rest.share.rsp.Response;
import java.util.Collections;
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

        final String hostname = lookupParams.hostname;
        AbsRestDns.Statistics stat = new AbsRestDns.Statistics(rsp.ips, rsp.clientIp, rsp.ttl);
        stat.errorCode = ErrorCode.SUCCESS;
        mCache.add(hostname, new LookupResult<>(rsp.ips, stat));

        PendingTasks pendingTasks = mHostnamePendingTasksMap.get(hostname);
        if (null != pendingTasks) {
            if (null != pendingTasks.removeExpiredCacheTask) {
                DnsExecutors.MAIN.cancel(pendingTasks.removeExpiredCacheTask);
                pendingTasks.removeExpiredCacheTask = null;
            }
            if (null != pendingTasks.asyncLookupTask) {
                DnsExecutors.MAIN.cancel(pendingTasks.asyncLookupTask);
                pendingTasks.asyncLookupTask = null;
            }
        } else {
            pendingTasks = new PendingTasks();
        }
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
        if (lookupParams.enableAsyncLookup) {
            int origLookUpFamily = lookupParams.family;
            final int lookupFamily = mDns.getDescription().family;
            final LookupParameters<LookupExtra> newLookupParams;
            if (!lookupParams.fallback2Local && origLookUpFamily == lookupFamily &&
                    !lookupParams.netChangeLookup) {
                newLookupParams = lookupParams;
            } else {
                newLookupParams =
                        new LookupParameters.Builder<>(lookupParams)
                                .fallback2Local(false)
                                .family(lookupFamily)
                                .networkChangeLookup(false)
                                .build();
            }
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
                            if (lookupResult.stat.lookupSuccess() || lookupResult.stat.lookupFailed()) {
                                // 异步解析成功取消对应的缓存清理任务
                                DnsExecutors.MAIN.cancel(removeExpiredCacheTask);
                                mPendingTasks.remove(removeExpiredCacheTask);
                            }
                        }
                    });
                    mPendingTasks.remove(this);
                }
            };
            pendingTasks.asyncLookupTask = asyncLookupTask;
            mPendingTasks.add(asyncLookupTask);
            DnsExecutors.MAIN.schedule(
                    asyncLookupTask, (long) (ASYNC_LOOKUP_FACTOR * rsp.ttl * 1000));
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
                        DnsLog.d("Network changed, clear caches");
                        mCache.clear();
                        synchronized (mPendingTasks) {
                            for (Runnable task : mPendingTasks) {
                                DnsExecutors.MAIN.cancel(task);
                            }
                        }
                        DnsLog.d("Network changed, refetch ThreeNets Ips");
                        BackupResolver.getInstance().getThreeNets();
                        synchronized (mAsyncLookupParamsSet) {
                            DnsLog.d("Network changed, enable async lookup");
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
                                        AsyncLookupResultQueue.enqueue(
                                                DnsManager.lookupWrapper(newLookupParams));
                                    }
                                });
                                asyncLookupParamsIterator.remove();
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
