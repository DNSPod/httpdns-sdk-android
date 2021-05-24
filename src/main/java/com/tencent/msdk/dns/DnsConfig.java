package com.tencent.msdk.dns;

import android.text.TextUtils;
import android.util.Log;
import com.tencent.msdk.dns.base.compat.CollectionCompat;
import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.log.ILogNode;
import com.tencent.msdk.dns.base.report.IReporter;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class DnsConfig {

    public final int logLevel;

    public final String appId;
    public String userId;

    public final boolean initBuiltInReporters;

    public final String dnsIp;

    public final LookupExtra lookupExtra;

    public final int timeoutMills;

    /* @Nullable */ public final Set<WildcardDomain> protectedDomains;
    /* @Nullable */ public final Set<String> preLookupDomains;
    /* @Nullable */ public final Set<String> asyncLookupDomains;

    public final String channel;
    public final boolean blockFirst;

    /* @Nullable */ public final DnsExecutors.ExecutorSupplier executorSupplier;

    /* @Nullable */ public final ILookedUpListener lookedUpListener;
    /* @Nullable */ public final List<ILogNode> logNodes;

    /**
     * @hide
     */
    /* @Nullable */ public final List<IReporter> reporters;

    private DnsConfig(int logLevel,
                      String appId, String userId, boolean initBuiltInReporters,
                      String dnsIp, String dnsId, String dnsKey, String token,
                      int timeoutMills,
                      Set<WildcardDomain> protectedDomains,
                      Set<String> preLookupDomains, Set<String> asyncLookupDomains,
                      String channel, boolean blockFirst,
                      DnsExecutors.ExecutorSupplier executorSupplier,
                      ILookedUpListener lookedUpListener, List<ILogNode> logNodes,
                      List<IReporter> reporters) {
        this.logLevel = logLevel;
        this.appId = appId;
        this.userId = userId;
        this.initBuiltInReporters = initBuiltInReporters;
        this.dnsIp = dnsIp;
        this.lookupExtra = new LookupExtra(dnsId, dnsKey, token);
        this.timeoutMills = timeoutMills;
        this.protectedDomains = protectedDomains;
        this.preLookupDomains = preLookupDomains;
        this.asyncLookupDomains = asyncLookupDomains;
        this.channel = channel;
        this.blockFirst = blockFirst;
        this.executorSupplier = executorSupplier;
        this.lookedUpListener = lookedUpListener;
        this.logNodes = logNodes;
        this.reporters = reporters;
    }

    boolean needProtect(/* @Nullable */String hostname) {
        if (TextUtils.isEmpty(hostname) || TextUtils.isEmpty(hostname = hostname.trim())) {
            return false;
        }

        if (null == protectedDomains) {
            return true;
        }
        for (WildcardDomain wildcardDomain : protectedDomains) {
            if (wildcardDomain.contains(hostname)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DnsConfig{" +
                "logLevel=" + logLevel +
                ", appId='" + appId + '\'' +
                ", userId='" + userId + '\'' +
                ", lookupExtra=" + lookupExtra +
                ", timeoutMills=" + timeoutMills +
                ", protectedDomains=" + CommonUtils.toString(protectedDomains) +
                ", preLookupDomains=" + CommonUtils.toString(preLookupDomains) +
                ", asyncLookupDomains=" + CommonUtils.toString(asyncLookupDomains) +
                ", channel='" + channel + '\'' +
                ", blockFirst=" + blockFirst +
                ", executorSupplier=" + executorSupplier +
                ", lookedUpListener=" + lookedUpListener +
                ", logNodes=" + CommonUtils.toString(logNodes) +
                ", reporters=" + CommonUtils.toString(reporters) +
                '}';
    }

    /* @VisibleForTesting */
    static class WildcardDomain {

        private final boolean mIsWildcard;
        private final String mNakedDomain;

        // 上层保证wildcardDomain已经trim
        private WildcardDomain(String wildcardDomain) {
            int lastWildcardIndex = wildcardDomain.lastIndexOf("*.");
            if (-1 == lastWildcardIndex) {
                mNakedDomain = wildcardDomain;
                mIsWildcard = false;
            } else {
                mNakedDomain = wildcardDomain.substring(lastWildcardIndex + 2);
                mIsWildcard = true;
            }
        }

        // 上层保证hostname已经trim
        /* @VisibleForTesting */
        boolean contains(String hostname) {
            if (mIsWildcard) {
                return hostname.endsWith(mNakedDomain);
            }
            return mNakedDomain.equals(hostname);
        }

        @Override
        public String toString() {
            return "WildcardDomain{" +
                    "mIsWildcard=" + mIsWildcard +
                    ", mNakedDomain='" + mNakedDomain + '\'' +
                    '}';
        }
    }

    /**
     * DnsConfig建造类
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final class Builder {

        private static final int DEFAULT_MAX_NUM_OF_PRE_LOOKUP_DOMAINS = 10;

        private int mLogLevel = Log.WARN;

        private String mAppId = "";
        private String mUserId = "";

        // CHANGE: 默认不初始化灯塔的key
        private boolean mInitBuiltInReporters = false;

        private String mDnsIp = "";
        private String mDnsId = "";
        private String mDnsKey = "";
        private String mToken = "";

        private int mTimeoutMills = 1000;

        private int mMaxNumOfPreLookupDomains = DEFAULT_MAX_NUM_OF_PRE_LOOKUP_DOMAINS;

        // mAsyncLookupDomains包含于mPreLookupDomains, mPreLookupDomains包含于mProtectedDomains

        private Set<WildcardDomain> mProtectedDomains = null;
        private Set<String> mPreLookupDomains = null;
        private Set<String> mAsyncLookupDomains = null;

        private String mChannel = Const.DES_HTTP_CHANNEL;
        private boolean mBlockFirst = false;

        private DnsExecutors.ExecutorSupplier mExecutorSupplier = null;

        private ILookedUpListener mLookedUpListener = null;
        private List<ILogNode> mLogNodes = null;

        private List<IReporter> mReporters = null;

        /**
         * 设置最低日志等级, 低于设置等级的日志不会输出
         * SDK默认仅将日志通过logcat输出, tag统一使用HTTPDNS
         * 不设置时, 默认输出<a href="https://developer.android.google.cn/reference/android/util/Log.html#WARN">WARN</a>及以上等级的日志
         *
         * @param logLevel 最低日志等级, 使用<a href="https://developer.android.google.cn/reference/android/util/Log">Log</a>类定义的常量, 可选值为
         *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#VERBOSE">VERBOSE</a>,
         *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#DEBUG">DEBUG</a>,
         *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#INFO">INFO</a>,
         *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#WARN">WARN</a>,
         *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#ERROR">ERROR</a>,
         *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#ASSERT">ASSERT</a>,
         *                 其中<a href="https://developer.android.google.cn/reference/android/util/Log.html#ASSERT">ASSERT</a>即不输出任何日志
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder logLevel(int logLevel) {
            mLogLevel = logLevel;
            return this;
        }

        /**
         * 设置AppId, 进行数据上报时用于区分业务
         *
         * @param appId AppId, 即灯塔AppId, 从<a href="https://console.cloud.tencent.com/HttpDNS">腾讯云官网</a>申请获得
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException appId为空时抛出
         */
        public Builder appId(String appId) {
            if (TextUtils.isEmpty(appId)) {
                throw new IllegalArgumentException("appId".concat(Const.EMPTY_TIPS));
            }
            mAppId = appId;
            return this;
        }

        /**
         * 设置UserId, 进行数据上报时区分用户, 出现问题时, 依赖该Id进行单用户问题排查
         *
         * @param userId UserId, 用户的唯一标识符, 腾讯业务建议直接使用OpenId, 腾讯云客户建议传入长度50位以内, 由字母数字下划线组合而成的字符串
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException userId为空时抛出
         */
        public Builder userId(String userId) {
            if (TextUtils.isEmpty(userId)) {
                throw new IllegalArgumentException("userId".concat(Const.EMPTY_TIPS));
            }
            mUserId = userId;
            return this;
        }

        /**
         * 自动初始化内置上报通道
         * 不设置时, 默认为自动初始化内置上报通道
         *
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder initBuiltInReporters() {
            mInitBuiltInReporters = true;
            return this;
        }

        /**
         * 不自动初始化内置上报通道
         * 不设置时, 默认为自动初始化内置上报通道
         *
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder notInitBuiltInReporters() {
            mInitBuiltInReporters = false;
            return this;
        }

        /**
         * 设置DnsIp
         *
         * @param dnsIp HTTPDNS IP 地址
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException dnsIp为空时抛出
         */
        public Builder dnsIp(String dnsIp) {
            if (TextUtils.isEmpty(dnsIp)) {
                throw new IllegalArgumentException("dnsIp".concat(Const.EMPTY_TIPS));
            }
            mDnsIp = dnsIp;
            return this;
        }

        /**
         * 设置DnsId
         *
         * @param dnsId DnsId, 即HTTPDNS服务的授权Id, 从<a href="https://console.cloud.tencent.com/HttpDNS">腾讯云官网</a>申请获得
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException dnsId为空时抛出
         */
        public Builder dnsId(String dnsId) {
            if (TextUtils.isEmpty(dnsId)) {
                throw new IllegalArgumentException("dnsId".concat(Const.EMPTY_TIPS));
            }
            mDnsId = dnsId;
            return this;
        }

        /**
         * 设置DnsKey
         *
         * @param dnsKey dnsKey, 即HTTPDNS服务的授权Id对应的加密密钥, 从<a href="https://console.cloud.tencent.com/HttpDNS">腾讯云官网</a>申请获得
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException dnsKey为空时抛出
         */
        public Builder dnsKey(String dnsKey) {
            if (TextUtils.isEmpty(dnsKey)) {
                throw new IllegalArgumentException("dnsKey".concat(Const.EMPTY_TIPS));
            }
            mDnsKey = dnsKey;
            return this;
        }

        /**
         * 设置Token
         *
         * @param token https使用的标识
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException token为空时抛出
         */
        public Builder token(String token) {
            if (TextUtils.isEmpty(token)) {
                throw new IllegalArgumentException("token".concat(Const.EMPTY_TIPS));
            }
            mToken = token;
            return this;
        }


        /**
         * 设置域名解析请求超时时间
         * 不设置时, 默认为1000ms
         *
         * @param timeoutMills 域名解析请求超时时间, 单位为ms
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException timeoutMills小于0时抛出
         */
        public Builder timeoutMills(int timeoutMills) {
            if (0 >= timeoutMills) {
                throw new IllegalArgumentException("timeoutMills".concat(Const.LESS_THAN_0_TIPS));
            }
            mTimeoutMills = timeoutMills;
            return this;
        }

        /**
         * @hide
         */
        public Builder maxNumOfPreLookupDomains(int maxNumOfPreLookupDomains) {
            if (0 >= maxNumOfPreLookupDomains) {
                throw new IllegalArgumentException(
                        "maxNumOfPreLookupDomains".concat(Const.LESS_THAN_0_TIPS));
            }
            mMaxNumOfPreLookupDomains = maxNumOfPreLookupDomains;
            return this;
        }

        /**
         * 设置域名白名单, 仅在白名单范围内的域名会使用HTTPDNS服务进行域名解析
         * 不设置时, 默认所有域名都会使用HTTPDNS服务进行域名解析
         *
         * @param domains 域名白名单, 支持通配符形式进行配置, 如"*.qq.com"即以qq.com为后缀的域名都允许使用HTTPDNS服务进行域名解析
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException domains为空时抛出
         */
        public synchronized Builder protectedDomains(String... domains) {
            if (CommonUtils.isEmpty(domains)) {
                throw new IllegalArgumentException("domains".concat(Const.EMPTY_TIPS));
            }

            if (null == mProtectedDomains) {
                mProtectedDomains = CollectionCompat.createSet(domains.length);
            }

            for (String domain : domains) {
                if (TextUtils.isEmpty(domain) || TextUtils.isEmpty(domain = domain.trim())) {
                    throw new IllegalArgumentException("domain".concat(Const.EMPTY_TIPS));
                }

                mProtectedDomains.add(new WildcardDomain(domain));
            }

            if (null != mPreLookupDomains) {
                Iterator<String> domainIterator = mPreLookupDomains.iterator();
                while (domainIterator.hasNext()) {
                    String domain = domainIterator.next();
                    boolean contains = false;
                    for (WildcardDomain wildcardDomain : mProtectedDomains) {
                        if (wildcardDomain.contains(domain)) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        domainIterator.remove();
                    }
                }
            }

            return this;
        }

        /**
         * 设置预解析域名, 预解析域名在SDK初始化时会通过后台线程进行一次静默解析
         * 不设置时, 默认不会进行预解析
         *
         * @param domains 预解析域名, 建议不要设置太多预解析域名, 当前限制为最多10个域名
         *                预解析域名应该包含在域名白名单之内
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException domains为空时抛出
         */
        public synchronized Builder preLookupDomains(String... domains) {
            if (CommonUtils.isEmpty(domains)) {
                throw new IllegalArgumentException("domains".concat(Const.EMPTY_TIPS));
            }

            if (null == mPreLookupDomains) {
                mPreLookupDomains = CollectionCompat.createSet(domains.length);
            }

            int numOfPreLookupDomains = mPreLookupDomains.size();
            // 避免for循环中每次都执行一次if判断
            if (null != mProtectedDomains) {
                for (String domain : domains) {
                    if (TextUtils.isEmpty(domain) || TextUtils.isEmpty(domain = domain.trim())) {
                        throw new IllegalArgumentException("domain".concat(Const.EMPTY_TIPS));
                    }

                    boolean contains = false;
                    for (WildcardDomain wildcardDomain : mProtectedDomains) {
                        if (wildcardDomain.contains(domain)) {
                            contains = true;
                            break;
                        }
                    }
                    if (contains) {
                        mPreLookupDomains.add(domain);
                        numOfPreLookupDomains++;
                    }

                    if (mMaxNumOfPreLookupDomains <= numOfPreLookupDomains) {
                        break;
                    }
                }
            } else {
                for (String domain : domains) {
                    if (TextUtils.isEmpty(domain) || TextUtils.isEmpty(domain = domain.trim())) {
                        throw new IllegalArgumentException("domain".concat(Const.EMPTY_TIPS));
                    }

                    mPreLookupDomains.add(domain);
                    numOfPreLookupDomains++;

                    if (mMaxNumOfPreLookupDomains <= numOfPreLookupDomains) {
                        break;
                    }
                }
            }

            if (null != mAsyncLookupDomains) {
                Iterator<String> domainIterator = mAsyncLookupDomains.iterator();
                while (domainIterator.hasNext()) {
                    String domain = domainIterator.next();
                    if (!mPreLookupDomains.contains(domain)) {
                        domainIterator.remove();
                    }
                }
            }

            return this;
        }

        /**
         * 设置异步解析域名, 异步解析域名在解析缓存即将过期时会通过后台线程进行静默解析
         * 不设置时, 默认不会进行异步解析
         *
         * @param domains 异步解析域名
         *                异步解析域名应该包含在预解析域名之内
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException domains为空时抛出
         */
        public synchronized Builder asyncLookupDomains(String... domains) {
            if (CommonUtils.isEmpty(domains)) {
                throw new IllegalArgumentException("domains".concat(Const.EMPTY_TIPS));
            }

            if (null == mAsyncLookupDomains) {
                mAsyncLookupDomains = CollectionCompat.createSet(domains.length);
            }

            // 避免for循环中每次都执行一次if判断
            if (null != mPreLookupDomains) {
                for (String domain : domains) {
                    if (TextUtils.isEmpty(domain) || TextUtils.isEmpty(domain = domain.trim())) {
                        throw new IllegalArgumentException("domain".concat(Const.EMPTY_TIPS));
                    }

                    if (mPreLookupDomains.contains(domain)) {
                        mAsyncLookupDomains.add(domain);
                    }
                }
            } else {
                int numOfAsyncLookupDomains = mAsyncLookupDomains.size();
                for (String domain : domains) {
                    if (TextUtils.isEmpty(domain) || TextUtils.isEmpty(domain = domain.trim())) {
                        throw new IllegalArgumentException("domain".concat(Const.EMPTY_TIPS));
                    }

                    mAsyncLookupDomains.add(domain);
                    numOfAsyncLookupDomains++;

                    if (mMaxNumOfPreLookupDomains <= numOfAsyncLookupDomains) {
                        break;
                    }
                }
            }

            return this;
        }

        public Builder aesHttp() {
            mChannel = Const.AES_HTTP_CHANNEL;
            return this;
        }

        public Builder desHttp() {
            mChannel = Const.DES_HTTP_CHANNEL;
            return this;
        }

        public Builder https() {
            mChannel = Const.HTTPS_CHANNEL;
            return this;
        }

        /**
         * 优先通过阻塞方式进行域名解析
         * 不设置时, 默认优先通过非阻塞方式进行域名解析
         *
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder blockFirst() {
            mBlockFirst = true;
            return this;
        }

        /**
         * 优先通过非阻塞方式进行域名解析
         * 不设置时, 默认优先通过非阻塞方式进行域名解析
         *
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder nonBlockFirst() {
            mBlockFirst = false;
            return this;
        }

        /**
         * 设置{@link DnsExecutors.ExecutorSupplier}, 用于为SDK定制线程池
         * 不设置时, 默认使用<a href="https://developer.android.com/reference/android/os/AsyncTask.html#THREAD_POOL_EXECUTOR">THREAD_POOL_EXECUTOR</a>作为SDK内部使用的线程池
         *
         * @param executorSupplier {@link DnsExecutors.ExecutorSupplier}接口实现类实例, SDK通过{@link DnsExecutors.ExecutorSupplier#get()}获取SDK内部使用的线程池
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException executorSupplier为null时抛出
         */
        public Builder executorSupplier(DnsExecutors.ExecutorSupplier executorSupplier) {
            if (null == executorSupplier) {
                throw new IllegalArgumentException(
                        "executorSupplier".concat(Const.NULL_POINTER_TIPS));
            }
            mExecutorSupplier = executorSupplier;
            return this;
        }

        /**
         * 设置{@link ILookedUpListener}, 用于监控SDK的解析情况
         *
         * @param lookedUpListener {@link ILookedUpListener}接口实现类实例
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException lookedUpListener为null时抛出
         */
        public Builder lookedUpListener(ILookedUpListener lookedUpListener) {
            if (null == lookedUpListener) {
                throw new IllegalArgumentException(
                        "lookedUpListener".concat(Const.NULL_POINTER_TIPS));
            }
            mLookedUpListener = lookedUpListener;
            return this;
        }

        /**
         * 添加{@link ILogNode}, 用于接收SDK输出的日志
         *
         * @param logNode {@link ILogNode}接口实现类实例
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException logNode为null时抛出
         */
        public synchronized Builder logNode(ILogNode logNode) {
            if (null == logNode) {
                throw new IllegalArgumentException("logNode".concat(Const.NULL_POINTER_TIPS));
            }
            if (null == mLogNodes) {
                mLogNodes = new ArrayList<>();
            }
            mLogNodes.add(logNode);
            return this;
        }

        /**
         * @hide
         */
        public synchronized Builder reporter(IReporter reporter) {
            if (null == reporter) {
                throw new IllegalArgumentException("reporter".concat(Const.NULL_POINTER_TIPS));
            }
            if (null == mReporters) {
                mReporters = new ArrayList<>();
            }
            mReporters.add(reporter);
            return this;
        }

        /**
         * 构建DnsConfig实例
         *
         * @return DnsConfig实例
         */
        public DnsConfig build() {
            return new DnsConfig(mLogLevel,
                    mAppId, mUserId, mInitBuiltInReporters, mDnsIp, mDnsId, mDnsKey,mToken,
                    mTimeoutMills,
                    mProtectedDomains, mPreLookupDomains, mAsyncLookupDomains,
                    mChannel, mBlockFirst,
                    mExecutorSupplier,
                    mLookedUpListener, mLogNodes,
                    mReporters);
        }
    }
}
