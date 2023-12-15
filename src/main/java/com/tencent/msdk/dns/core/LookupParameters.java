package com.tencent.msdk.dns.core;

import android.content.Context;
import android.text.TextUtils;

import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.base.utils.NetworkStack;

public final class LookupParameters<LookupExtra extends IDns.ILookupExtra> {

    // NOTE: channel和lookupExtra必须保持一致
    // 即channel对应的dns实现类必须实现IDns<LookupExtra>(即泛型参数应该为当前LookupParameters的LookupExtra)

    public final Context appContext;

    public final String hostname;

    public final int timeoutMills;
    public final String dnsIp;
    public final LookupExtra lookupExtra;

    public final String channel;
    public final boolean fallback2Local;
    public final boolean blockFirst;

    public final int family;
    public final boolean ignoreCurNetStack;
    public final int customNetStack;

    // TODO(zefeng): 分离enableAsyncLookup以及asyncLookup
    public final boolean enableAsyncLookup;

    public final int curRetryTime;
    public final boolean netChangeLookup;

    public String requestHostname;

    private LookupParameters(
            Context appContext,
            String hostname, int timeoutMills, String dnsIp, LookupExtra lookupExtra,
            String channel, boolean fallback2Local, boolean blockFirst,
            int family, boolean ignoreCurNetStack, int customNetStack, boolean enableAsyncLookup,
            int curRetryTime, boolean netChangeLookup) {
        this.appContext = appContext;
        this.hostname = hostname;
        this.timeoutMills = timeoutMills;
        this.dnsIp = dnsIp;
        this.lookupExtra = lookupExtra;
        this.channel = channel;
        this.fallback2Local = fallback2Local;
        this.blockFirst = blockFirst;
        this.family = family;
        this.ignoreCurNetStack = ignoreCurNetStack;
        this.customNetStack = customNetStack;
        this.enableAsyncLookup = enableAsyncLookup;
        this.curRetryTime = curRetryTime;
        this.netChangeLookup = netChangeLookup;
        setRequestHostname(hostname);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LookupParameters<?> that = (LookupParameters<?>) o;
        return timeoutMills == that.timeoutMills
                && fallback2Local == that.fallback2Local
                && blockFirst == that.blockFirst
                && family == that.family
                && ignoreCurNetStack == that.ignoreCurNetStack
                && customNetStack == that.customNetStack
                && enableAsyncLookup == that.enableAsyncLookup
                && curRetryTime == that.curRetryTime
                && netChangeLookup == that.netChangeLookup
                && CommonUtils.equals(appContext, that.appContext)
                && CommonUtils.equals(hostname, that.hostname)
                && CommonUtils.equals(dnsIp, that.dnsIp)
                && CommonUtils.equals(lookupExtra, that.lookupExtra)
                && CommonUtils.equals(channel, that.channel);
    }

    public void setRequestHostname(String hostname) {
        this.requestHostname = hostname;
    }

    @Override
    public int hashCode() {
        return CommonUtils.hash(appContext, hostname, timeoutMills, dnsIp, lookupExtra, channel,
                fallback2Local, blockFirst, family, ignoreCurNetStack, customNetStack, enableAsyncLookup,
                curRetryTime, netChangeLookup);
    }

    @Override
    public String toString() {
        return "LookupParameters{"
                + "appContext=" + appContext
                + ", hostname='" + hostname + '\''
                + ", timeoutMills=" + timeoutMills
                + ", dnsIp=" + dnsIp
                + ", lookupExtra=" + lookupExtra
                + ", channel='" + channel + '\''
                + ", fallback2Local=" + fallback2Local
                + ", blockFirst=" + blockFirst
                + ", family=" + family
                + ", ignoreCurNetStack=" + ignoreCurNetStack
                + ", customNetStack=" + customNetStack
                + ", enableAsyncLookup=" + enableAsyncLookup
                + ", curRetryTime=" + curRetryTime
                + ", netChangeLookup=" + netChangeLookup
                + '}';
    }

    public static final class Builder<LookupExtra extends IDns.ILookupExtra> {

        private Context mAppContext;

        private String mHostname;

        private int mTimeoutMills = Const.INVALID_TIMEOUT_MILLS;
        private String mDnsIp;
        private LookupExtra mLookupExtra;

        private String mChannel;
        private boolean mFallback2Local = true;
        private boolean mBlockFirst = false;

        private int mFamily = DnsDescription.Family.UN_SPECIFIC;
        private boolean mIgnoreCurNetStack = false;
        private int mCustomNetStack = 0;

        private boolean mEnableAsyncLookup = false;

        private int mCurRetryTime = 0;
        private boolean mNetChangeLookup = false;

        public Builder() {
        }

        public Builder(LookupParameters<LookupExtra> lookupParams) {
            mAppContext = lookupParams.appContext;
            mHostname = lookupParams.hostname;
            mTimeoutMills = lookupParams.timeoutMills;
            mDnsIp = lookupParams.dnsIp;
            mLookupExtra = lookupParams.lookupExtra;
            mChannel = lookupParams.channel;
            mFallback2Local = lookupParams.fallback2Local;
            mBlockFirst = lookupParams.blockFirst;
            mFamily = lookupParams.family;
            mIgnoreCurNetStack = lookupParams.ignoreCurNetStack;
            mCustomNetStack = lookupParams.customNetStack;
            mEnableAsyncLookup = lookupParams.enableAsyncLookup;
            mCurRetryTime = lookupParams.curRetryTime;
            mNetChangeLookup = lookupParams.netChangeLookup;
        }

        public Builder<LookupExtra> context(Context context) {
            if (null == context) {
                throw new IllegalArgumentException("context".concat(Const.NULL_POINTER_TIPS));
            }
            mAppContext = context.getApplicationContext();
            return this;
        }

        public Builder<LookupExtra> hostname(String hostname) {
            if (TextUtils.isEmpty(hostname)) {
                throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
            }
            mHostname = hostname;
            return this;
        }

        public Builder<LookupExtra> timeoutMills(int timeoutMills) {
            if (0 >= timeoutMills) {
                throw new IllegalArgumentException("timeoutMills".concat(Const.LESS_THAN_0_TIPS));
            }
            mTimeoutMills = timeoutMills;
            return this;
        }

        public Builder<LookupExtra> dnsIp(String dnsIp) {
            if (TextUtils.isEmpty(dnsIp)) {
                throw new IllegalArgumentException("dnsIp".concat(Const.EMPTY_TIPS));
            }
            mDnsIp = dnsIp;
            return this;
        }

        public Builder<LookupExtra> lookupExtra(LookupExtra lookupExtra) {
            if (null == lookupExtra) {
                throw new IllegalArgumentException("lookupExtra".concat(Const.NULL_POINTER_TIPS));
            }
            mLookupExtra = lookupExtra;
            return this;
        }

        public Builder<LookupExtra> channel(String channel) {
            if (TextUtils.isEmpty(channel)) {
                throw new IllegalArgumentException("channel".concat(Const.EMPTY_TIPS));
            }
            mChannel = channel;
            return this;
        }

        public Builder<LookupExtra> fallback2Local(boolean fallback2Local) {
            mFallback2Local = fallback2Local;
            return this;
        }

        public Builder<LookupExtra> blockFirst(boolean blockFirst) {
            mBlockFirst = blockFirst;
            return this;
        }

        public Builder<LookupExtra> family(int family) {
            if (DnsDescription.isFamilyInvalid(family)) {
                throw new IllegalArgumentException("family".concat(Const.INVALID_TIPS));
            }

            mFamily = family;
            return this;
        }

        public Builder<LookupExtra> ignoreCurrentNetworkStack(boolean ignoreCurNetStack) {
            mIgnoreCurNetStack = ignoreCurNetStack;
            return this;
        }

        public Builder<LookupExtra> customNetStack(int customNetStack) {
            if (NetworkStack.isInvalid(customNetStack)) {
                throw new IllegalArgumentException("customNetStack".concat(Const.INVALID_TIPS));
            }

            mCustomNetStack = customNetStack;
            return this;
        }

        public Builder<LookupExtra> enableAsyncLookup(boolean enableAsyncLookup) {
            mEnableAsyncLookup = enableAsyncLookup;
            return this;
        }

        public Builder<LookupExtra> curRetryTime(int curRetryTime) {
            if (0 > curRetryTime) {
                throw new IllegalArgumentException("curRetryTime".concat(Const.LESS_THAN_0_TIPS));
            }

            mCurRetryTime = curRetryTime;
            return this;
        }

        public Builder<LookupExtra> networkChangeLookup(boolean netChangeLookup) {
            mNetChangeLookup = netChangeLookup;
            return this;
        }

        public LookupParameters<LookupExtra> build() {
            if (null == mAppContext) {
                throw new IllegalStateException("mAppContext".concat(Const.NOT_INIT_TIPS));
            }
            if (null == mHostname) {
                throw new IllegalStateException("mHostname".concat(Const.NOT_INIT_TIPS));
            }
            if (Const.INVALID_TIMEOUT_MILLS == mTimeoutMills) {
                throw new IllegalStateException("mTimeoutMills".concat(Const.NOT_INIT_TIPS));
            }
            if (null == mDnsIp) {
                throw new IllegalStateException("mDnsIp".concat(Const.NOT_INIT_TIPS));
            }
            if (null == mLookupExtra) {
                throw new IllegalStateException("mLookupExtra".concat(Const.NOT_INIT_TIPS));
            }
            if (null == mChannel) {
                throw new IllegalStateException("mChannel".concat(Const.NOT_INIT_TIPS));
            }

            return new LookupParameters<>(mAppContext,
                    mHostname, mTimeoutMills, mDnsIp, mLookupExtra,
                    mChannel, mFallback2Local, mBlockFirst,
                    mFamily, mIgnoreCurNetStack, mCustomNetStack, mEnableAsyncLookup,
                    mCurRetryTime, mNetChangeLookup);
        }
    }
}
