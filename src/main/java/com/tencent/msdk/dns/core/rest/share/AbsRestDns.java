package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.LookupContext;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.share.rsp.Response;
import com.tencent.msdk.dns.core.stat.AbsStatistics;

import java.io.Serializable;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责缓存管理
 */
public abstract class AbsRestDns implements IDns<LookupExtra> {

    protected static final int TCP_CONTINUOUS_RCV_BUF_SIZE = 1024;
    protected static final int RCV_ZERO_MAX = 128;

    protected final CacheHelper mCacheHelper = new CacheHelper(this);

    // NOTE: stat: 结果参数
    protected boolean tryGetResultFromCache(
            LookupParameters<LookupExtra> lookupParams, Statistics stat) {
        if (null == lookupParams) {
            throw new IllegalArgumentException("lookupParams".concat(Const.EMPTY_TIPS));
        }
        if (null == stat) {
            throw new IllegalArgumentException("stat".concat(Const.NULL_POINTER_TIPS));
        }

        // 初始化，保活域名刷新不命中缓存
        if (lookupParams.enableAsyncLookup) {
            return false;
        }

        final String[] hostnameArr = lookupParams.hostname.split(",");
        Map<String, Object> result = handleHostnameCached(hostnameArr);

        String[] cachedIps = (String[]) result.get("ips");
        StringBuilder requestHostname = (StringBuilder) result.get("requestHostname");
        boolean cached = (boolean) result.get("cached");

        stat.ips = cachedIps;
        if (DnsService.getDnsConfig().useExpiredIpEnable) {
            // 乐观DNS以requestHostname来判断过期域名,requestHostname不存在时，无过期域名。
            lookupParams.setRequestHostname(requestHostname.toString());
        } else if (requestHostname.length() > 0) {
            lookupParams.setRequestHostname(requestHostname.toString());
        }

        if (cached) {
            stat.cached = true;
            stat.errorCode = ErrorCode.SUCCESS;
            DnsLog.d("Lookup for %s, cache hit", lookupParams.hostname);
            return true;
        }

        if (cachedIps.length > 0) {
            stat.hadPartCachedIps = true;
        }

        return false;
    }

    private Map<String, Object> handleHostnameCached(String[] hostnameArr) {
        boolean cached = true;
        String[] ips = Const.EMPTY_IPS;
        String[] tempIps;
        // 未命中缓存的请求域名&乐观DNS场景下，缓存过期需要请求的域名
        StringBuilder requestHostname = new StringBuilder();
        Map<String, Object> result = new HashMap<>();
        List<String> cachedIps = new ArrayList<>();
        for (String hostname : hostnameArr) {
            LookupResult lookupResult = mCacheHelper.get(hostname);
            if (null != lookupResult && !CommonUtils.isEmpty(tempIps = lookupResult.ipSet.ips)) {
                if (hostnameArr.length > 1) {
                    for (String ip : tempIps) {
                        cachedIps.add(hostname + ":" + ip);
                    }
                } else {
                    ips = tempIps;
                }
                Statistics cachedStat = (Statistics) lookupResult.stat;

                if (DnsService.getDnsConfig().useExpiredIpEnable
                        && cachedStat.expiredTime < System.currentTimeMillis()) {
                    requestHostname.append(hostname).append(',');
                }
            } else {
                cached = false;
                requestHostname.append(hostname).append(',');
            }
        }

        requestHostname = new StringBuilder(requestHostname.length() > 0 ? requestHostname.substring(0,
                requestHostname.length() - 1) : "");
        if (cachedIps.size() > 0) {
            ips = cachedIps.toArray(new String[cachedIps.size()]);
        }

        result.put("requestHostname", requestHostname);
        result.put("ips", ips);
        result.put("cached", cached);

        return result;
    }

    @Override
    public LookupResult getResultFromCache(LookupParameters<LookupExtra> lookupParams) {
        Statistics stat = new Statistics();
        stat.retryTimes = lookupParams.curRetryTime;
        stat.asyncLookup = lookupParams.enableAsyncLookup;
        stat.netChangeLookup = lookupParams.netChangeLookup;
        stat.startLookup();

        tryGetResultFromCache(lookupParams, stat);
        stat.endLookup();
        return new LookupResult<>(stat.ips, stat);
    }

    /**
     * @hide 负责亲子关系管理，token管理
     * 子类负责请求响应的具体实现，channel(DatagramChannel/SocketChannel/...)的管理和session实例的创建
     */
    public abstract class AbsSession implements IDns.ISession {

        protected int mState = State.CREATED;

        protected LookupContext<LookupExtra> mLookupContext;

        protected final IDns mDns;

        protected SelectionKey mSelectionKey = null;

        protected final Statistics mStat = new Statistics();

        /* @Nullable */ private final AbsSession mParent;
        private List<AbsSession> mChildren = Collections.emptyList();

        public AbsSession(LookupContext<LookupExtra> lookupContext, IDns dns, AbsSession parent) {
            if (null == lookupContext) {
                throw new IllegalArgumentException("lookupContext".concat(Const.NULL_POINTER_TIPS));
            }
            if (null == dns) {
                throw new IllegalArgumentException("dns".concat(Const.NULL_POINTER_TIPS));
            }

            mStat.startLookup();
            mStat.retryTimes = lookupContext.curRetryTime();
            mStat.asyncLookup = lookupContext.enableAsyncLookup();
            mStat.netChangeLookup = lookupContext.networkChangeLookup();

            mLookupContext = lookupContext;
            mDns = dns;
            mParent = parent;

            // NOTE: 先判断是否命中缓存, 改变状态
            // 获取解析结果统一由receiveResponse完成
            if (!lookupContext.enableAsyncLookup() && null != mCacheHelper.get(lookupContext.hostname())) {
                mState = State.READABLE;
            }
        }

        @Override
        public void connect() {
            if (State.CONNECTABLE != mState) {
                return;
            }
            int connectRes = NonBlockResult.NON_BLOCK_RESULT_FAILED;
            try {
                connectRes = connectInternal();
            } finally {
                if (connectRes != NonBlockResult.NON_BLOCK_RESULT_NEED_CONTINUE
                        && State.ENDED != mState) {
                    mState = State.WRITABLE;
                }
            }
        }

        @Override
        public final void request() {
            if (State.WRITABLE != mState) {
                return;
            }
            int requestRes = NonBlockResult.NON_BLOCK_RESULT_FAILED;
            try {
                requestRes = requestInternal();
            } finally {
                if (requestRes != NonBlockResult.NON_BLOCK_RESULT_NEED_CONTINUE && State.ENDED != mState) {
                    mState = State.READABLE;
                }
            }
        }

        @Override
        public final String[] receiveResponse() {
            // 使用while实现类似goto的效果
            // 这里如果抽取为一个私有method，可以避开这种奇怪的写法，但感觉职责不太明确
            //noinspection LoopStatementThatDoesntLoop,ConstantConditions
            if (State.READABLE != mState) {
                DnsLog.d("HttpDns(%d) mState is not readable", mDns.getDescription().family);
                return mStat.ips;
            }

            Response rsp = Response.EMPTY;
            LookupParameters<LookupExtra> lookupParameters = mLookupContext.asLookupParameters();
            try {
                if (tryGetResultFromCache(lookupParameters, mStat)) {
                    return mStat.ips;
                }

                rsp = responseInternal();

                if ((rsp == Response.EMPTY || rsp == Response.NEED_CONTINUE || rsp.ips.length == 0)
                        && mStat.statusCode == 200
                        || mStat.statusCode == 401) {
                    mCacheHelper.clearErrorRspCache(lookupParameters.requestHostname);
                }

                if (mStat.errorCode == ErrorCode.SUCCESS) {
                    mCacheHelper.put(lookupParameters, rsp);
                }

                mStat.clientIp = rsp.clientIp;
                mStat.ttl = rsp.ttl;
                mStat.expiredTime = mStat.getExpiredTime(rsp.ttl);
                mStat.ips = rsp.ips;
            } finally {
                if (rsp != Response.NEED_CONTINUE) {
                    end();
                    // NOTE: syncState()不直接在end()中调用, 并非每个end都需要syncState
                    syncState();
                }
            }
            return CommonUtils.templateIps(mStat.ips, lookupParameters);
        }

        @Override
        public final void end() {
            if (State.ENDED == mState) {
                return;
            }
            mState = State.ENDED;
            mStat.endLookup();
            endInternal();
        }

        @Override
        public final ISession copy() {
            AbsSession child = copyInternal();
            if (Collections.<AbsSession>emptyList() == mChildren) {
                mChildren = new ArrayList<>();
            }
            mChildren.add(child);
            return child;
        }

        @Override
        public final IDns getDns() {
            return mDns;
        }

        @Override
        public final boolean isEnd() {
            return State.ENDED == mState;
        }

        @Override
        public IStatistics getStatistics() {
            return mStat;
        }

        // return:
        //       NonBlockResult.NON_BLOCK_RESULT_FAILED
        //       NonBlockResult.NON_BLOCK_RESULT_SUCCESS
        //       NonBlockResult.NON_BLOCK_RESULT_NEED_CONTINUE
        protected abstract int connectInternal();

        // return:
        //       NonBlockResult.NON_BLOCK_RESULT_FAILED
        //       NonBlockResult.NON_BLOCK_RESULT_SUCCESS
        //       NonBlockResult.NON_BLOCK_RESULT_NEED_CONTINUE
        protected abstract int requestInternal();

        // return:
        //       Response.EMPTY
        //       Response.NEED_CONTINUE
        //       Other valid Response
        protected abstract Response responseInternal();

        protected abstract void endInternal();

        protected abstract AbsSession copyInternal();

        private void syncState() {
            if (State.ENDED != mState) {
                return;
            }
            if (null != mParent) {
                mParent.end();
            }
            for (AbsSession child : mChildren) {
                child.end();
            }
        }

        public class Token implements IToken {

            @Override
            public boolean isConnectable() {
                // NOTE: 直接命中缓存情况下, mSelectionKey为null
                if (null == mSelectionKey) {
                    return State.CONNECTABLE == mState;
                }
                if (!mSelectionKey.isValid()) {
                    end();
                    return false;
                }
                return State.CONNECTABLE == mState && mSelectionKey.isConnectable();
            }

            @Override
            public boolean tryFinishConnect() {
                return true;
            }

            @Override
            public boolean isReadable() {
                // NOTE: 直接命中缓存情况下, mSelectionKey为null
                if (null == mSelectionKey) {
                    return State.READABLE == mState;
                }
                if (!mSelectionKey.isValid()) {
                    end();
                    return false;
                }
                return State.READABLE == mState && mSelectionKey.isReadable();
            }

            @Override
            public boolean isWritable() {
                // NOTE: 直接命中缓存情况下, mSelectionKey为null
                if (null == mSelectionKey) {
                    return State.WRITABLE == mState;
                }
                if (!mSelectionKey.isValid()) {
                    end();
                    return false;
                }
                return State.WRITABLE == mState && mSelectionKey.isWritable();
            }

            @Override
            public boolean isAvailable() {
                if (null == mSelectionKey) {
                    return State.CREATED == mState;
                }
                if (!mSelectionKey.isValid()) {
                    end();
                    return false;
                }
                return true;
            }
        }
    }

    /**
     * HTTPDNS域名解析统计数据类
     */
    public static class Statistics extends AbsStatistics implements Serializable {
        private static final long serialVersionUID = 8621285648054627787L;

        public static final Statistics NOT_LOOKUP = new Statistics();

        static {
            NOT_LOOKUP.errorCode = ErrorCode.NOT_LOOKUP;
        }

        /**
         * 是否仅部分命中缓存（使用于批量解析中）
         */
        public boolean hadPartCachedIps = false;

        /**
         * 域名解析错误码
         */
        public int errorCode = ErrorCode.LOOKUP_TIMEOUT;
        /**
         * 域名解析错误信息
         */
        public String errorMsg = " ";

        /**
         * 客户端公网IP
         */
        public String clientIp = Const.INVALID_IP;
        /**
         * 解析结果TTL(缓存有效时间), 单位s
         */
        public transient Map<String, Integer> ttl = new HashMap<>();

        public long expiredTime = 0;
        /**
         * 域名解析重试次数
         */
        public int retryTimes = 0;
        /**
         * 是否命中缓存
         */
        public boolean cached = false;

        /**
         * 是否是异步解析
         */
        public boolean asyncLookup = false;
        /**
         * 是否是切网导致的异步解析
         */
        public boolean netChangeLookup = false;

        public int statusCode;

        public Statistics() {
        }

        Statistics(String[] ips, String clientIp, Map<String, Integer> ttl) {
            if (null == ips) {
                throw new IllegalArgumentException("ips".concat(Const.NULL_POINTER_TIPS));
            }
            if (TextUtils.isEmpty(clientIp)) {
                throw new IllegalArgumentException("clientIp".concat(Const.EMPTY_TIPS));
            }
            if (ttl.isEmpty()) {
                throw new IllegalArgumentException("ttl".concat(Const.INVALID_TIPS));
            }

            this.ips = ips;
            this.clientIp = clientIp;
            this.ttl = ttl;
            this.expiredTime = getExpiredTime(ttl);
        }

        public long getExpiredTime(Map<String, Integer> ttl) {
            if (ttl.isEmpty()) {
                return 0;
            }
            int min = Const.MAX_DEFAULT_TTL;
            for (String key : ttl.keySet()) {
                int value = ttl.get(key);
                if (!Response.isTtlInvalid(value)) {
                    min = Math.min(value, min);
                }
            }
            return System.currentTimeMillis() + min * 1000L;
        }

        @Override
        public boolean lookupPartCached() {
            return hadPartCachedIps;
        }

        @Override
        public boolean lookupSuccess() {
            return Const.EMPTY_IPS != ips;
        }

        @Override
        public String toString() {
            return "Statistics{"
                    + "errorCode=" + errorCode
                    + ", errorMsg='" + errorMsg + '\''
                    + ", statusCode=" + statusCode
                    + ", clientIp='" + clientIp + '\''
                    + ", ttl=" + ttl
                    + ", expiredTime=" + expiredTime
                    + ", retryTimes=" + retryTimes
                    + ", cached=" + cached
                    + ", asyncLookup=" + asyncLookup
                    + ", netChangeLookup=" + netChangeLookup
                    + ", ips=" + Arrays.toString(ips)
                    + ", costTimeMills=" + costTimeMills
                    + ", startLookupTimeMills=" + startLookupTimeMills
                    + '}';
        }
    }
}
