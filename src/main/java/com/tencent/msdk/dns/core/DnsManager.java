package com.tencent.msdk.dns.core;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.tencent.msdk.dns.base.compat.CollectionCompat;
import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.NetworkStack;
import com.tencent.msdk.dns.core.local.LocalDns;
import com.tencent.msdk.dns.core.rest.aeshttp.AesHttpDns;
import com.tencent.msdk.dns.core.rest.deshttp.DesHttpDns;
import com.tencent.msdk.dns.core.rest.https.HttpsDns;
import com.tencent.msdk.dns.core.retry.Retry;
import com.tencent.msdk.dns.core.sorter.Sorter;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;
import com.tencent.msdk.dns.core.stat.StatisticsMergeFactory;
import com.tencent.msdk.dns.report.CacheStatisticsReport;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class DnsManager {

    private static final long SYSTEM_CALL_INTERVAL_MILLS = 10;
    private static final float AWAIT_FOR_RUNNING_LOOKUP_FACTOR = 1.2f;

    private static final Map<String, DnsGroup> CHANNEL_DNS_GROUP_MAP = CollectionCompat.createMap();

    private static final Map<LookupParameters, LookupLatchResultPair> RUNNING_LOOKUP_LATCH_MAP =
            new ConcurrentHashMap<>();

    private static ISorter.IFactory sSorterFactory = new Sorter.Factory();
    private static IRetry sRetry = new Retry();
    private static IStatisticsMerge.IFactory sStatMergeFactory = new StatisticsMergeFactory();

    private static volatile ILookupListener sLookupListener = null;

    static {
        registerDns(new LocalDns());
        registerDns(new DesHttpDns(DnsDescription.Family.INET));
        registerDns(new DesHttpDns(DnsDescription.Family.INET6));
        registerDns(new DesHttpDns(DnsDescription.Family.UN_SPECIFIC));
        registerDns(new AesHttpDns(DnsDescription.Family.INET));
        registerDns(new AesHttpDns(DnsDescription.Family.INET6));
        registerDns(new AesHttpDns(DnsDescription.Family.UN_SPECIFIC));
        registerDns(new HttpsDns(DnsDescription.Family.INET));
        registerDns(new HttpsDns(DnsDescription.Family.INET6));
        registerDns(new HttpsDns(DnsDescription.Family.UN_SPECIFIC));
    }

    @SuppressWarnings("WeakerAccess")
    public static synchronized void registerDns(IDns dns) {
        if (null == dns) {
            throw new IllegalArgumentException("dns".concat(Const.NULL_POINTER_TIPS));
        }

        String channel = dns.getDescription().channel;
        DnsGroup dnsGroup;
        if (CHANNEL_DNS_GROUP_MAP.containsKey(channel)) {
            dnsGroup = CHANNEL_DNS_GROUP_MAP.get(channel);
        } else {
            dnsGroup = new DnsGroup();
            CHANNEL_DNS_GROUP_MAP.put(channel, dnsGroup);
        }
        switch (dns.getDescription().family) {
            case DnsDescription.Family.UN_SPECIFIC:
                dnsGroup.mUnspecDns = dns;
                break;
            case DnsDescription.Family.INET:
                dnsGroup.mInetDns = dns;
                break;
            case DnsDescription.Family.INET6:
                dnsGroup.mInet6Dns = dns;
                break;
            default:
        }
    }

    @SuppressWarnings("unused")
    public static synchronized void unregisterAllDns() {
        CHANNEL_DNS_GROUP_MAP.clear();
    }

    public static void setLookupListener(/* @Nullable */ILookupListener lookupListener) {
        sLookupListener = lookupListener;
    }

    public static <LookupExtraT extends IDns.ILookupExtra>
    LookupResult<IStatisticsMerge> getResultFromCache(LookupParameters<LookupExtraT> lookupParams) {
        DnsGroup restDnsGroup = CHANNEL_DNS_GROUP_MAP.get(lookupParams.channel);
        if (restDnsGroup == null) {
            return new LookupResult<IStatisticsMerge>(IpSet.EMPTY, new StatisticsMerge(lookupParams.appContext));
        }
        LookupExtraT lookupExtra = lookupParams.lookupExtra;

        LookupContext<LookupExtraT> lookupContext = LookupContext.wrap(lookupParams);
        if (!NetworkStack.isInvalid(lookupParams.customNetStack) && lookupParams.customNetStack > 0) {
            lookupContext.currentNetworkStack(lookupParams.customNetStack);
        } else {
            lookupContext.currentNetworkStack(NetworkStack.get());
        }

        int currentNetworkStack = lookupContext.currentNetworkStack();
        ISorter sorter = sSorterFactory.create(currentNetworkStack);
        lookupContext.sorter(sorter);
        // snapshot
        @SuppressWarnings("unchecked") IStatisticsMerge<LookupExtraT> statMerge =
                sStatMergeFactory.create((Class<LookupExtraT>) lookupExtra.getClass(), lookupParams.appContext);
        lookupContext.statisticsMerge(statMerge);

        IDns dns;
        switch (currentNetworkStack) {
            case 1:
                dns = restDnsGroup.mInetDns;
                break;
            case 2:
                dns = restDnsGroup.mInet6Dns;
                break;
            default:
                dns = restDnsGroup.mUnspecDns;
                break;
        }

        LookupResult lookupResultFromCache = dns.getResultFromCache(lookupParams);
        statMerge.statContext(lookupContext);
        if (lookupResultFromCache.stat.lookupSuccess() || lookupResultFromCache.stat.lookupPartCached()) {
            lookupContext.sorter().put(dns, lookupResultFromCache.ipSet.ips);
            lookupContext.statisticsMerge().merge(dns, lookupResultFromCache.stat);

            IpSet ipSet = sorter.sort();
            statMerge.statResult(ipSet);
            LookupResult<IStatisticsMerge> lookupResult = new LookupResult<IStatisticsMerge>(ipSet, statMerge);
            DnsLog.d("getResultFromCache by httpdns cache:" + lookupResult.ipSet + "; " + lookupResult.stat);
            return lookupResult;
        }

        return new LookupResult<IStatisticsMerge>(IpSet.EMPTY, statMerge);
    }

    // lookupParameters创建时会进行参数校验
    public static <LookupExtraT extends IDns.ILookupExtra>
    LookupResult<IStatisticsMerge> lookup(LookupParameters<LookupExtraT> lookupParams) {
        if (null == lookupParams) {
            throw new IllegalArgumentException("lookupParams".concat(Const.NULL_POINTER_TIPS));
        }

        // NOTE: 考虑同步检查正在运行的解析任务的代码块
        DnsLog.v("DnsManager.lookup(%s) called", lookupParams);

        long startTimeMills = SystemClock.elapsedRealtime();
        LookupLatchResultPair lookupLatchResultPair = RUNNING_LOOKUP_LATCH_MAP.get(lookupParams);
        if (null != lookupLatchResultPair) {
            return getResultFromLatch(lookupParams, lookupLatchResultPair, startTimeMills);
        }
        // 初始化CountDownLatch同步计数器
        CountDownLatch lookupLatch = new CountDownLatch(1);
        LookupResultHolder lookupResultHolder = new LookupResultHolder();
        RUNNING_LOOKUP_LATCH_MAP.put(lookupParams, new LookupLatchResultPair(lookupLatch, lookupResultHolder));

        final LookupExtraT lookupExtra = lookupParams.lookupExtra;
        String channel = lookupParams.channel;
        boolean fallback2Local = lookupParams.fallback2Local;
        LookupContext<LookupExtraT> lookupContext = LookupContext.wrap(lookupParams);

        DnsGroup localDnsGroup = null;
        DnsGroup restDnsGroup = null;
        if (fallback2Local) {
            localDnsGroup = CHANNEL_DNS_GROUP_MAP.get(Const.LOCAL_CHANNEL);
        }
        if (Const.LOCAL_CHANNEL.equals(channel)) {
            localDnsGroup = CHANNEL_DNS_GROUP_MAP.get(Const.LOCAL_CHANNEL);
        } else {
            restDnsGroup = CHANNEL_DNS_GROUP_MAP.get(channel);
        }
        if (!NetworkStack.isInvalid(lookupParams.customNetStack) && lookupParams.customNetStack > 0) {
            lookupContext.currentNetworkStack(lookupParams.customNetStack);
        } else {
            lookupContext.currentNetworkStack(NetworkStack.get());
        }
        ISorter sorter = sSorterFactory.create(lookupContext.currentNetworkStack());
        lookupContext.sorter(sorter);
        // snapshot
        @SuppressWarnings("unchecked") IStatisticsMerge<LookupExtraT> statMerge =
                sStatMergeFactory.create((Class<LookupExtraT>) lookupExtra.getClass(), lookupParams.appContext);
        lookupContext.statisticsMerge(statMerge);

        CountDownManager.Transaction transaction = CountDownManager.beginTransaction();
        lookupContext.transaction(transaction);
        Set<IDns> dnses = Collections.synchronizedSet(CollectionCompat.<IDns>createSet());
        lookupContext.dnses(dnses);
        // NOTE: sessions仅会被当前线程访问
        List<IDns.ISession> sessions = new ArrayList<>();
        lookupContext.sessions(sessions);
        try {
            // 暂时不忽略LocalDns解析结果(RestDns解析失败时，超时时间内会等待LocalDns解析结果)
            if (null != restDnsGroup) {
                // 先查缓存，有其一即可
                final LookupResult<IStatisticsMerge> lookupResultFromCache = getResultFromCache(lookupParams);
                DnsLog.d("getResultFromCache: " + lookupResultFromCache);
                if (lookupResultFromCache.stat.lookupSuccess()) {
                    if (lookupResultFromCache.stat.lookupPartCached()) {
                        // 仅部分命中缓存
                        lookupContext.sorter().putPartCache(lookupResultFromCache.ipSet);
                        // 收集命中缓存的数据
                        DnsExecutors.WORK.execute(new Runnable() {
                            @Override
                            public void run() {
                                CacheStatisticsReport.add(lookupResultFromCache);
                            }
                        });

                    } else {
                        lookupResultHolder.mLookupResult = lookupResultFromCache;
                        DnsLog.d("DnsManager lookup getResultFromCache success");
                        return lookupResultFromCache;
                    }
                }
                // statContext操作在读完缓存之后，下发解析请求前。结果会更新在lookupResult上
                statMerge.statContext(lookupContext);

                // 打开Selector
                prepareTasks(restDnsGroup, lookupContext);
                if (!lookupContext.allDnsLookedUp() && null != localDnsGroup) {
                    prepareTasks(localDnsGroup, lookupContext);
                }
            } else if (null != localDnsGroup) {
                prepareTasks(localDnsGroup, lookupContext);
            }

            CountDownLatch countDownLatch = transaction.commit();
            lookupContext.countDownLatch(countDownLatch);

            return getResultForSelector(countDownLatch, lookupContext, lookupParams, lookupResultHolder, sessions,
                    startTimeMills);
        } finally {
            // 结束超时的session, 统计收尾
            endSessions(lookupContext);
            lookupLatch.countDown();
            RUNNING_LOOKUP_LATCH_MAP.remove(lookupParams);
            DnsLog.d("FINALLY statMerge: %s", statMerge.toJsonResult());

            // 解析完成, 清理lookupContext
            clearEndedSession(lookupContext);
            dnses.clear();
            // API 19以上DatagramSocket才实现了Closeable接口
            Selector selector = lookupContext.selector();
            if (null != selector) {
                try {
                    selector.close();
                    DnsLog.d("%s closed", selector);
                } catch (IOException ignored) {
                    DnsLog.e("exception: %s", ignored);
                }
            }
        }
    }

    private static LookupResult<IStatisticsMerge>
    getResultFromLatch(LookupParameters lookupParams, @NonNull LookupLatchResultPair lookupLatchResultPair,
                       long startTimeMills) {
        DnsLog.d("The same lookup task(for %s) is running, just wait for it", lookupParams);
        CountDownLatch lookupLatch = lookupLatchResultPair.mLookupLatch;
        try {
            if (lookupLatch.await((long) (AWAIT_FOR_RUNNING_LOOKUP_FACTOR * lookupParams.timeoutMills),
                    TimeUnit.MILLISECONDS)) {
                // NOTE: await之后mLookupResult不为null
                return lookupLatchResultPair.mLookupResultHolder.mLookupResult;
            }
            DnsLog.d("Await for running lookup for %s timeout", lookupParams);
            return new LookupResult<IStatisticsMerge>(IpSet.EMPTY, new StatisticsMerge(lookupParams.appContext));
        } catch (Exception e) {
            DnsLog.w(e, "Await for running lookup for %s failed", lookupParams);
            int fixedTimeoutMills =
                    (int) (lookupParams.timeoutMills - (SystemClock.elapsedRealtime() - startTimeMills));
            if (0 < fixedTimeoutMills) {
                return lookup(new LookupParameters.Builder<>(lookupParams).timeoutMills(fixedTimeoutMills).build());
            }
            return new LookupResult<IStatisticsMerge>(IpSet.EMPTY, new StatisticsMerge(lookupParams.appContext));
        }
    }

    private static <LookupExtraT extends IDns.ILookupExtra>
    LookupResult<IStatisticsMerge> getResultForNullSelector(CountDownLatch countDownLatch,
                                                            LookupContext lookupContext,
                                                            LookupParameters lookupParams,
                                                            LookupResultHolder lookupResultHolder,
                                                            long startTimeMills) {
        IRetry retry = sRetry;
        int maxRetryTimes = retry.maxRetryTimes();
        int timeoutMills = lookupParams.timeoutMills;
        int remainTimeMills = timeoutMills - (int) (SystemClock.elapsedRealtime() - startTimeMills);
        int waitTimeMills = 0 < maxRetryTimes ? remainTimeMills / (maxRetryTimes + 1) : remainTimeMills;
        int retriedTimes = 0;

        DnsLog.d("selector is null");
        // 仅阻塞解析
        // NOTE: 非localDNS解析进行countDownLatch,HDNS不被localDNS阻塞
        while (countDownLatch.getCount() > 0
                && SystemClock.elapsedRealtime() - startTimeMills < timeoutMills) {
            try {
                countDownLatch.await(waitTimeMills, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
                DnsLog.d(e, "sessions not empty, but exception");
            }
            if (countDownLatch.getCount() > 0
                    // 需要重试
                    && canRetry(startTimeMills, timeoutMills, maxRetryTimes, retriedTimes)) {
                retriedTimes++;
                remainTimeMills = timeoutMills - (int) (SystemClock.elapsedRealtime() - startTimeMills);
                LookupContext<LookupExtraT> newLookupContext =
                        lookupContext.newLookupContext(
                                new LookupParameters.Builder<>(lookupParams)
                                        .timeoutMills(remainTimeMills)
                                        .curRetryTime(retriedTimes)
                                        .build());
                retry.retryBlock(newLookupContext);
            }
        }
        ISorter sorter = lookupContext.sorter();
        StatisticsMerge statMerge = (StatisticsMerge) lookupContext.statisticsMerge();
        IpSet ipSet = sorter.sort();
        statMerge.statResult(ipSet);
        LookupResult<IStatisticsMerge> lookupResult = new LookupResult<IStatisticsMerge>(ipSet, statMerge);
        lookupResultHolder.mLookupResult = lookupResult;
        return lookupResult;
    }

    private static <LookupExtraT extends IDns.ILookupExtra>
    LookupResult<IStatisticsMerge> getResultForSelector(CountDownLatch countDownLatch,
                                                        LookupContext lookupContext,
                                                        LookupParameters lookupParams,
                                                        LookupResultHolder lookupResultHolder,
                                                        List<IDns.ISession> sessions,
                                                        long startTimeMills) {
        Selector selector = lookupContext.selector();
        if (selector == null) {
            return getResultForNullSelector(countDownLatch, lookupContext, lookupParams, lookupResultHolder,
                    startTimeMills);
        }
        IRetry retry = sRetry;
        int maxRetryTimes = retry.maxRetryTimes();
        int timeoutMills = lookupParams.timeoutMills;
        int remainTimeMills = timeoutMills - (int) (SystemClock.elapsedRealtime() - startTimeMills);
        int waitTimeMills = 0 < maxRetryTimes ? remainTimeMills / (maxRetryTimes + 1) : remainTimeMills;
        int retriedTimes = 0;

        // 非阻塞解析
        // TODO: sessions加上对于解析结果可以忽略的支持(主要是支持LocalDns)
        while (!sessions.isEmpty() && SystemClock.elapsedRealtime() - startTimeMills < timeoutMills) {
            // sleep以降低系统调用频率
            try {
                Thread.sleep(SYSTEM_CALL_INTERVAL_MILLS);
            } catch (Exception ignored) {
                DnsLog.e("exception: %s", ignored);
            }
            try {
                DnsLog.d("selector %s wait for sessions:%d, mills:%d", selector, sessions.size(), waitTimeMills);
                selector.select(waitTimeMills);
            } catch (Exception e) {
                DnsLog.d(e, "sessions not empty, but exception");
            }
            // Socket进行请求
            tryLookup(lookupContext);
            if (!sessions.isEmpty()
                    // 需要重试
                    && canRetry(startTimeMills, timeoutMills, maxRetryTimes, retriedTimes)) {
                DnsLog.d("sessions is not empty, sessions:%d, enter retry", sessions.size());
                retriedTimes++;
                LookupContext<LookupExtraT> newLookupContext = lookupContext.newLookupContext(
                        new LookupParameters.Builder<>(lookupParams)
                                .curRetryTime(retriedTimes)
                                .build());
                retry.retryNonBlock(newLookupContext);
            }
        }
        // 阻塞解析
        remainTimeMills = timeoutMills - (int) (SystemClock.elapsedRealtime() - startTimeMills);
        try {
            if (sessions.size() > 0) {
                DnsLog.d("selector wait for last timeout if sessions is not empty, sessions:%d, mills:%d",
                        sessions.size(), waitTimeMills);
            }
            countDownLatch.await(remainTimeMills, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            DnsLog.e("exception: %s", ignored);
        }
        ISorter sorter = lookupContext.sorter();
        StatisticsMerge statMerge = (StatisticsMerge) lookupContext.statisticsMerge();
        IpSet ipSet = sorter.sort();
        statMerge.statResult(ipSet);
        LookupResult<IStatisticsMerge> lookupResult = new LookupResult<IStatisticsMerge>(ipSet, statMerge);
        lookupResultHolder.mLookupResult = lookupResult;
        return lookupResult;
    }

    public static <LookupExtraT extends IDns.ILookupExtra>
    LookupResult<IStatisticsMerge> lookupWrapper(LookupParameters<LookupExtraT> lookupParams) {
        LookupResult<IStatisticsMerge> lookupResult = lookup(lookupParams);
        DnsLog.d("LookupResult %s", lookupResult.ipSet);
        if (null != sLookupListener) {
            sLookupListener.onLookedUp(lookupParams, lookupResult);
        }
        return lookupResult;
    }

    private static <LookupExtraT extends IDns.ILookupExtra>
    void prepareTasks(DnsGroup dnsGroup, LookupContext<LookupExtraT> lookupContext) {
        int curNetStack = lookupContext.currentNetworkStack();
//        int family = lookupContext.family();
        boolean ignoreCurNetStack = lookupContext.ignoreCurrentNetworkStack();

        // ignoreCurNetStack = true / localdns, 双栈同时请求
        if (null != dnsGroup.mUnspecDns
                && (ignoreCurNetStack || curNetStack == NetworkStack.DUAL_STACK
                || dnsGroup.mUnspecDns instanceof LocalDns)) {
            //noinspection unchecked
            prepareTask((IDns<LookupExtraT>) dnsGroup.mUnspecDns, lookupContext);
        } else if (null != dnsGroup.mInetDns
                // 异步解析不关注当前网络栈
                && (ignoreCurNetStack || curNetStack == NetworkStack.IPV4_ONLY)) {
            //noinspection unchecked
            prepareTask((IDns<LookupExtraT>) dnsGroup.mInetDns, lookupContext);
        } else if (null != dnsGroup.mInet6Dns
                // 异步解析不关注当前网络栈
                && (ignoreCurNetStack || curNetStack == NetworkStack.IPV6_ONLY)) {
            //noinspection unchecked
            prepareTask((IDns<LookupExtraT>) dnsGroup.mInet6Dns, lookupContext);
        }

    }

    private static <LookupExtraT extends IDns.ILookupExtra>
    void prepareTask(final IDns<LookupExtraT> dns, LookupContext<LookupExtraT> lookupContext) {
        DnsLog.d("prepareTask:" + dns);
        lookupContext.dnses().add(dns);
        if (lookupContext.blockFirst()
                // NOTE: 临时方案, 避免LocalDns冗余创建Selector
                || Const.LOCAL_CHANNEL.equals(dns.getDescription().channel)) {
            LookupHelper.prepareBlockLookupTask(dns, lookupContext);
            return;
        }
        IDns.ISession session;
        // HTTPS情况下也采用HTTPCONNECTION做请求
        if (!Const.HTTPS_CHANNEL.equals(lookupContext.channel()) && ((null != lookupContext.selector())
                || tryOpenSelector(lookupContext)) && null != (session = dns.getSession(lookupContext))) {
            LookupHelper.prepareNonBlockLookupTask(session, lookupContext);
        } else {
            LookupHelper.prepareBlockLookupTask(dns, lookupContext);
        }
    }

    private static boolean tryOpenSelector(LookupContext lookupContext) {
        try {
            Selector selector = Selector.open();
            lookupContext.selector(selector);
            DnsLog.d("%s opened", selector);
            return true;
        } catch (Exception e) {
            DnsLog.d(e, "Open selector failed");
            return false;
        }
    }

    private static <LookupExtraT extends IDns.ILookupExtra> void tryLookup(LookupContext<LookupExtraT> lookupContext) {
        Iterator<IDns.ISession> sessionIterator = lookupContext.sessions().iterator();
        while (sessionIterator.hasNext()) {
            IDns.ISession session = sessionIterator.next();
            if (session.isEnd()) {
                continue;
            }
            IDns.ISession.IToken token = session.getToken();
            if (token.isReadable()) {
                DnsLog.d("%s event readable", session.getDns().getDescription());
                String[] ips = session.receiveResponse();
                IDns.IStatistics statistics = session.getStatistics();
                if (statistics.lookupSuccess() || statistics.lookupFailed()) {
                    IDns dns = session.getDns();
                    sessionIterator.remove();
                    lookupContext.dnses().remove(dns);
                    LookupHelper.lookupFinished(lookupContext, dns, statistics, ips);
                    continue;
                }
            } else if (token.isWritable()) {
                DnsLog.d("%s event writable", session.getDns().getDescription());
                // 发起请求
                session.request();
            } else {
                if (token.isConnectable()) {
                    DnsLog.d("%s event connectable", session.getDns().getDescription());
                    session.connect();
                }
                // 如果不是writable，则每次都需要finishConnect
                boolean finishConnectRes = token.tryFinishConnect();
                DnsLog.d("%s event finishConnect:%b", session.getDns().getDescription(), finishConnectRes);
            }

            // 每次检查下，及时清理
            if (!token.isAvailable()) {
                DnsLog.d("%s event not available, maybe closed", session.getDns().getDescription());
                IDns dns = session.getDns();
                sessionIterator.remove();
                lookupContext.dnses().remove(dns);
            }
        }
    }

    private static boolean canRetry(long startTimeMills, int timeoutMills, int maxRetryTimes, int retriedTimes) {
        return retriedTimes < maxRetryTimes
                // NOTE: 本次查询已经超时
                && (int) (SystemClock.elapsedRealtime() - startTimeMills)
                > timeoutMills * (retriedTimes + 1) / (maxRetryTimes + 1);
    }

    private static <LookupExtraT extends IDns.ILookupExtra>
    void endSessions(LookupContext<LookupExtraT> lookupContext) {
        for (IDns.ISession session : lookupContext.sessions()) {
            session.end();
            lookupContext.statisticsMerge().merge(session.getDns(), session.getStatistics());
        }
    }

    private static void clearEndedSession(LookupContext lookupContext) {
        @SuppressWarnings("unchecked") Iterator<IDns.ISession> sessionIterator = lookupContext.sessions().iterator();
        while (sessionIterator.hasNext()) {
            IDns.ISession session = sessionIterator.next();
            if (session.isEnd()) {
                sessionIterator.remove();
            }
        }
    }

    private static class DnsGroup {

        IDns mUnspecDns;
        IDns mInetDns;
        IDns mInet6Dns;
    }

    private static class LookupResultHolder {

        LookupResult<IStatisticsMerge> mLookupResult = null;
    }

    private static class LookupLatchResultPair {

        final CountDownLatch mLookupLatch;
        final LookupResultHolder mLookupResultHolder;

        public LookupLatchResultPair(CountDownLatch lookupLatch, LookupResultHolder lookupResultHolder) {
            if (null == lookupLatch) {
                throw new IllegalArgumentException("lookupLatch".concat(Const.NULL_POINTER_TIPS));
            }
            if (null == lookupResultHolder) {
                throw new IllegalArgumentException("lookupResultHolder".concat(Const.NULL_POINTER_TIPS));
            }

            mLookupLatch = lookupLatch;
            mLookupResultHolder = lookupResultHolder;
        }
    }
}
