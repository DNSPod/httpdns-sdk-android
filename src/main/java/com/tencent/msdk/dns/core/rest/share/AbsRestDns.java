package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.ICache;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.LookupContext;
import com.tencent.msdk.dns.core.LookupParameters;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.cache.Cache;
import com.tencent.msdk.dns.core.cache.Database;
import com.tencent.msdk.dns.core.ipRank.IpRankCallback;
import com.tencent.msdk.dns.core.ipRank.IpRankHelper;
import com.tencent.msdk.dns.core.ipRank.IpRankItem;
import com.tencent.msdk.dns.core.ipRank.IpRankTask;
import com.tencent.msdk.dns.core.rest.share.rsp.Response;
import com.tencent.msdk.dns.core.stat.AbsStatistics;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 负责缓存管理
 */
public abstract class AbsRestDns implements IDns<LookupExtra> {

    protected static final int TCP_CONTINUOUS_RCV_BUF_SIZE = 1024;
    protected static final int RCV_ZERO_MAX = 128;

    static ICache cache = DnsService.getDnsConfig().cachedIpEnable ? new Database() : new Cache();

    protected final CacheHelper mCacheHelper = new CacheHelper(this, cache);

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
        String hostname = lookupParams.hostname;
        LookupResult lookupResult = mCacheHelper.get(hostname);
        String[] ips;
        if (null != lookupResult && !CommonUtils.isEmpty(ips = lookupResult.ipSet.ips)) {
            Statistics cachedStat = (Statistics) lookupResult.stat;
            stat.errorCode = ErrorCode.SUCCESS;
            stat.clientIp = cachedStat.clientIp;
            stat.ttl = cachedStat.ttl;
            stat.expiredTime = cachedStat.expiredTime;
            stat.ips = ips;
            stat.cached = true;
            DnsLog.d("Lookup for %s, cache hit", hostname);
            return true;
        }
        return false;
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
     * @hide
     * 负责亲子关系管理，token管理
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
            if (!lookupContext.enableAsyncLookup() &&
                    null != mCacheHelper.get(lookupContext.hostname())) {
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
                if (connectRes != NonBlockResult.NON_BLOCK_RESULT_NEED_CONTINUE &&
                    State.ENDED != mState) {
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
                if (requestRes != NonBlockResult.NON_BLOCK_RESULT_NEED_CONTINUE &&
                    State.ENDED != mState) {
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
            try {
                if (tryGetResultFromCache(mLookupContext.asLookupParameters(), mStat)) {
                    return mStat.ips;
                }
                rsp = responseInternal();
                if (rsp != Response.EMPTY && rsp != Response.NEED_CONTINUE){
                    mStat.errorCode = ErrorCode.SUCCESS;
                    mCacheHelper.put(mLookupContext.asLookupParameters(), rsp);
                }
                mStat.clientIp = rsp.clientIp;
                mStat.ttl = rsp.ttl;
                mStat.expiredTime = System.currentTimeMillis() + rsp.ttl * 1000;
                mStat.ips = rsp.ips;
            } finally {
                if (rsp != Response.NEED_CONTINUE) {
                    end();
                    // NOTE: syncState()不直接在end()中调用, 并非每个end都需要syncState
                    syncState();
                }
            }
            return mStat.ips;
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
    public static class Statistics extends AbsStatistics {

        public static final Statistics NOT_LOOKUP = new Statistics();

        static {
            NOT_LOOKUP.errorCode = ErrorCode.NOT_LOOKUP;
        }

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
        public int ttl = Const.DEFAULT_TIME_INTERVAL;

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

        Statistics(String[] ips, String clientIp, int ttl) {
            if (null == ips) {
                throw new IllegalArgumentException("ips".concat(Const.NULL_POINTER_TIPS));
            }
            if (TextUtils.isEmpty(clientIp)) {
                throw new IllegalArgumentException("clientIp".concat(Const.EMPTY_TIPS));
            }
            if (Response.isTtlInvalid(ttl)) {
                throw new IllegalArgumentException("ttl".concat(Const.INVALID_TIPS));
            }

            this.ips = ips;
            this.clientIp = clientIp;
            this.ttl = ttl;
            this.expiredTime = System.currentTimeMillis() + ttl * 1000;
        }

        @Override
        public String toString() {
            return "Statistics{" +
                    "errorCode=" + errorCode +
                    ", errorMsg='" + errorMsg + '\'' +
                    ", statusCode=" + statusCode +
                    ", clientIp='" + clientIp + '\'' +
                    ", ttl=" + ttl +
                    ", expiredTime=" + expiredTime +
                    ", retryTimes=" + retryTimes +
                    ", cached=" + cached +
                    ", asyncLookup=" + asyncLookup +
                    ", netChangeLookup=" + netChangeLookup +
                    ", ips=" + Arrays.toString(ips) +
                    ", costTimeMills=" + costTimeMills +
                    ", startLookupTimeMills=" + startLookupTimeMills +
                    '}';
        }
    }
}
