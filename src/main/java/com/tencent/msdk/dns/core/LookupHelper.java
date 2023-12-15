package com.tencent.msdk.dns.core;

import com.tencent.msdk.dns.base.log.DnsLog;

import java.util.Set;

public final class LookupHelper {

    public static <LookupExtraT extends IDns.ILookupExtra>
    void prepareNonBlockLookupTask(IDns.ISession session, LookupContext<LookupExtraT> lookupContext) {
        prepareNonBlockLookupTask(session, lookupContext, false);
    }

    public static <LookupExtraT extends IDns.ILookupExtra>
    void prepareNonBlockLookupTask(IDns.ISession session, LookupContext<LookupExtraT> lookupContext, boolean forRetry) {
        if (null == session) {
            throw new IllegalArgumentException("session".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == lookupContext) {
            throw new IllegalArgumentException("lookupContext".concat(Const.NULL_POINTER_TIPS));
        }
        DnsLog.d("prepareNonBlockLookupTask call, forRetry:%b", forRetry);
        if (session.getToken().isReadable()) {
            DnsLog.d("prepareNonBlockLookupTask start receive");
            String[] ips = session.receiveResponse();
            if (session.getStatistics().lookupSuccess() || session.getStatistics().lookupFailed()) {
                IDns dns = session.getDns();
                if (!forRetry) {
                    lookupContext.sessions().remove(session);
                }
                lookupContext.dnses().remove(dns);
                if (session.getStatistics().lookupSuccess()) {
                    lookupContext.sorter().put(dns, ips);
                }
                lookupContext.statisticsMerge()
                        .merge(dns, session.getStatistics());
            }
        } else if (!forRetry) {
            lookupContext.sessions().add(session);
        }
    }

    public static <LookupExtraT extends IDns.ILookupExtra>
    void prepareBlockLookupTask(final IDns<LookupExtraT> dns,
                                final LookupContext<LookupExtraT> lookupContext) {
        if (null == dns) {
            throw new IllegalArgumentException("dns".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == lookupContext) {
            throw new IllegalArgumentException("lookupContext".concat(Const.NULL_POINTER_TIPS));
        }

        // LookupHelper是静态类，使用匿名内部类没有内存泄漏风险
        Runnable blockLookupTask = new Runnable() {
            @Override
            public void run() {
                Set<IDns> dnses = lookupContext.dnses();
                if (!dnses.contains(dns)) {
                    return;
                }
                LookupResult lookupResult = dns.lookup(lookupContext.asLookupParameters());
                if (lookupResult.stat.lookupSuccess() || lookupResult.stat.lookupFailed()) {
                    dnses.remove(dns);
                    if (lookupResult.stat.lookupSuccess()) {
                        lookupContext.sorter().put(dns, lookupResult.ipSet.ips);
                    }
                    // 不论是否成功都将stat进行合并，让正确的errorCode可以传出
                    lookupContext.statisticsMerge()
                            .merge(dns, lookupResult.stat);
                }

            }
        };

        // 只有非localDNS进入countDownLatch阻塞任务
        if (!Const.LOCAL_CHANNEL.equals(dns.getDescription().channel)) {
            lookupContext.transaction().addTask(blockLookupTask);
        } else {
            lookupContext.transaction().addTask(blockLookupTask, true);
        }
    }
}
