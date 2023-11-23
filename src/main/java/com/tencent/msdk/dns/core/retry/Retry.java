package com.tencent.msdk.dns.core.retry;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.IRetry;
import com.tencent.msdk.dns.core.LookupContext;
import com.tencent.msdk.dns.core.LookupHelper;

import java.util.List;
import java.util.Set;

public final class Retry implements IRetry {

    @Override
    public int maxRetryTimes() {
        return 1;
    }

    @Override
    public void retryNonBlock(LookupContext lookupContext) {
        if (null == lookupContext) {
            throw new IllegalArgumentException("lookupContext".concat(Const.NULL_POINTER_TIPS));
        }

        @SuppressWarnings("unchecked") List<IDns.ISession> sessions = lookupContext.sessions();
        DnsLog.d("Retry lookup for %s(%d) nonBlock session:%d  start",
                lookupContext.hostname(), lookupContext.family(), sessions.size());
        for (IDns.ISession session : sessions) {
            IDns.ISession retrySession = session.copy();
            LookupHelper.prepareNonBlockLookupTask(retrySession, lookupContext, true);
        }
        DnsLog.d("Retry lookup for %s(%d) nonBlock session:%d finish.",
                lookupContext.hostname(), lookupContext.family(), sessions.size());
    }

    @SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "unchecked"})
    @Override
    public <LookupExtraT extends IDns.ILookupExtra>
    void retryBlock(LookupContext<LookupExtraT> lookupContext) {
        if (null == lookupContext) {
            throw new IllegalArgumentException("lookupContext".concat(Const.NULL_POINTER_TIPS));
        }

        DnsLog.d("Retry lookup for %s(%d) block",
                lookupContext.hostname(), lookupContext.family());
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        Set<IDns> dnses = lookupContext.dnses();
        synchronized (dnses) {
            for (final IDns dns : dnses) {
                LookupHelper.prepareBlockLookupTask(dns, lookupContext);
            }
        }
        lookupContext.transaction().commit();
    }
}
