package com.tencent.msdk.dns.core;

import android.content.Context;

import com.tencent.msdk.dns.base.utils.NetworkStack;

import java.nio.channels.Selector;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public final class LookupContext<LookupExtra extends IDns.ILookupExtra> {

    private final LookupParameters<LookupExtra> mLookupParams;

    private int mCurNetStack = Const.INVALID_NETWORK_STACK;

    private ISorter mSorter;
    private IStatisticsMerge mStatMerge;

    private CountDownManager.Transaction mTransaction;
    private CountDownLatch mCountDownLatch;

    private Selector mSelector;
    private Set<IDns> mDnses;
    private List<IDns.ISession> mSessions;

    private LookupContext(LookupParameters<LookupExtra> lookupParams) {
        if (null == lookupParams) {
            throw new IllegalArgumentException("lookupParams".concat(Const.NULL_POINTER_TIPS));
        }
        mLookupParams = lookupParams;
    }


    public static <LookupExtra extends IDns.ILookupExtra>
    LookupContext<LookupExtra> wrap(LookupParameters<LookupExtra> lookupParams) {
        // 参数检查由构造方法完成
        return new LookupContext<>(lookupParams);
    }

    public LookupContext<LookupExtra>
    newLookupContext(LookupParameters<LookupExtra> lookupParams) {
        // NOTE: 参数检查由构造方法完成
        return wrap(lookupParams)
                .currentNetworkStack(mCurNetStack)
                .sorter(mSorter)
                .statisticsMerge(mStatMerge)
                .transaction(mTransaction)
                .countDownLatch(mCountDownLatch)
                .selector(mSelector)
                .dnses(mDnses)
                .sessions(mSessions);
    }

    public LookupParameters<LookupExtra> asLookupParameters() {
        return mLookupParams;
    }


    public Context appContext() {
        return mLookupParams.appContext;
    }

    public String hostname() {
        return mLookupParams.hostname;
    }

    public String requestHostname() {
        return mLookupParams.requestHostname;
    }

    public int timeoutMills() {
        return mLookupParams.timeoutMills;
    }

    public String dnsIp() {
        return mLookupParams.dnsIp;
    }

    public LookupExtra lookupExtra() {
        return mLookupParams.lookupExtra;
    }

    public String channel() {
        return mLookupParams.channel;
    }

    public boolean fallback2Local() {
        return mLookupParams.fallback2Local;
    }

    public boolean blockFirst() {
        return mLookupParams.blockFirst;
    }

    public int family() {
        return mLookupParams.family;
    }

    public boolean ignoreCurrentNetworkStack() {
        return mLookupParams.ignoreCurNetStack;
    }

    public boolean enableAsyncLookup() {
        return mLookupParams.enableAsyncLookup;
    }

    public int curRetryTime() {
        return mLookupParams.curRetryTime;
    }

    public boolean networkChangeLookup() {
        return mLookupParams.netChangeLookup;
    }


    public LookupContext<LookupExtra> currentNetworkStack(int curNetStack) {
        if (NetworkStack.isInvalid(curNetStack)) {
            throw new IllegalArgumentException("curNetStack".concat(Const.INVALID_TIPS));
        }
        mCurNetStack = curNetStack;
        return this;
    }

    public int currentNetworkStack() {
        if (NetworkStack.isInvalid(mCurNetStack)) {
            throw new IllegalStateException("mCurNetStack".concat(Const.NOT_INIT_TIPS));
        }
        return mCurNetStack;
    }


    public LookupContext<LookupExtra> sorter(ISorter sorter) {
        if (null == sorter) {
            throw new IllegalArgumentException("sorter".concat(Const.NULL_POINTER_TIPS));
        }
        mSorter = sorter;
        return this;
    }

    public ISorter sorter() {
        if (null == mSorter) {
            throw new IllegalStateException("mSorter".concat(Const.NOT_INIT_TIPS));
        }
        return mSorter;
    }

    public LookupContext<LookupExtra> statisticsMerge(IStatisticsMerge statMerge) {
        if (null == statMerge) {
            throw new IllegalArgumentException("statMerge".concat(Const.NULL_POINTER_TIPS));
        }
        mStatMerge = statMerge;
        return this;
    }

    public IStatisticsMerge statisticsMerge() {
        if (null == mStatMerge) {
            throw new IllegalStateException("mStatMerge".concat(Const.NOT_INIT_TIPS));
        }
        return mStatMerge;
    }


    public LookupContext<LookupExtra> transaction(CountDownManager.Transaction transaction) {
        if (null == transaction) {
            throw new IllegalArgumentException("transaction".concat(Const.NULL_POINTER_TIPS));
        }
        mTransaction = transaction;
        return this;
    }

    public CountDownManager.Transaction transaction() {
        if (null == mTransaction) {
            throw new IllegalStateException("mTransaction".concat(Const.NOT_INIT_TIPS));
        }
        return mTransaction;
    }

    public LookupContext<LookupExtra> countDownLatch(CountDownLatch countDownLatch) {
        if (null == countDownLatch) {
            throw new IllegalArgumentException("countDownLatch".concat(Const.NULL_POINTER_TIPS));
        }
        mCountDownLatch = countDownLatch;
        return this;
    }

    public CountDownLatch countDownLatch() {
        if (null == mCountDownLatch) {
            throw new IllegalStateException("mCountDownLatch".concat(Const.NOT_INIT_TIPS));
        }
        return mCountDownLatch;
    }


    public LookupContext<LookupExtra> selector(Selector selector) {
        mSelector = selector;
        return this;
    }

    public Selector selector() {
        return mSelector;
    }

    public LookupContext<LookupExtra> dnses(Set<IDns> dnses) {
        if (null == dnses) {
            throw new IllegalArgumentException("dnses".concat(Const.NULL_POINTER_TIPS));
        }
        mDnses = dnses;
        return this;
    }

    public Set<IDns> dnses() {
        if (null == mDnses) {
            throw new IllegalStateException("mDnses".concat(Const.NOT_INIT_TIPS));
        }
        return mDnses;
    }

    public boolean allDnsLookedUp() {
        if (null == mDnses) {
            throw new IllegalStateException("mDnses".concat(Const.NOT_INIT_TIPS));
        }

        return mDnses.isEmpty();
    }

    public LookupContext<LookupExtra> sessions(List<IDns.ISession> sessions) {
        if (null == sessions) {
            throw new IllegalArgumentException("sessions".concat(Const.NULL_POINTER_TIPS));
        }
        mSessions = sessions;
        return this;
    }

    public List<IDns.ISession> sessions() {
        if (null == mSessions) {
            throw new IllegalStateException("mSessions".concat(Const.NOT_INIT_TIPS));
        }
        return mSessions;
    }

    @Override
    public String toString() {
        return "LookupContext{" +
                "mLookupParams=" + mLookupParams +
                ", mCurNetStack=" + mCurNetStack +
                ", mSorter=" + mSorter +
                ", mStatMerge=" + mStatMerge +
                ", mTransaction=" + mTransaction +
                ", mCountDownLatch=" + mCountDownLatch +
                ", mSelector=" + mSelector +
                ", mDnses=" + mDnses +
                ", mSessions=" + mSessions +
                '}';
    }
}
