package com.tencent.msdk.dns;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.msdk.dns.base.compat.CollectionCompat;
import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.log.ILogNode;
import com.tencent.msdk.dns.base.report.IReporter;
import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.rank.IpRankItem;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class DnsConfig {

    public final int logLevel;

    public final String appId;
    public String userId;

    @Deprecated
    public final boolean initBuiltInReporters;

    public final LookupExtra lookupExtra;

    public final int timeoutMills;

    /* @Nullable */ public final Set<WildcardDomain> protectedDomains;
    /* @Nullable */ public final Set<String> preLookupDomains;
    /* @Nullable */ public final Set<String> persistentCacheDomains;
    public boolean enablePersistentCache;

    /* @Nullable */ public final Set<IpRankItem> ipRankItems;

    public final String channel;
    public boolean enableReport;
    public final boolean blockFirst;
    public final int customNetStack;

    /* @Nullable */ public final DnsExecutors.ExecutorSupplier executorSupplier;

    /* @Nullable */ public final ILookedUpListener lookedUpListener;
    /* @Nullable */ public final List<ILogNode> logNodes;

    /**
     * @hide
     */
    /* @Nullable */ public final List<IReporter> reporters;

    public boolean useExpiredIpEnable;

    public boolean cachedIpEnable;

    public boolean enableDomainServer = false;

    public String routeIp;

    private DnsConfig(int logLevel,
                      String appId, String userId, boolean initBuiltInReporters,
                      String dnsId, String dnsKey, String token,
                      int timeoutMills,
                      Set<WildcardDomain> protectedDomains,
                      Set<String> preLookupDomains, boolean enablePersistentCache, Set<String> persistentCacheDomains,
                      Set<IpRankItem> ipRankItems, String channel, boolean enableReport, boolean blockFirst,
                      int customNetStack, DnsExecutors.ExecutorSupplier executorSupplier,
                      ILookedUpListener lookedUpListener, List<ILogNode> logNodes,
                      List<IReporter> reporters, boolean useExpiredIpEnable, boolean cachedIpEnable, String routeIp) {
        this.logLevel = logLevel;
        this.appId = appId;
        this.userId = userId;
        this.initBuiltInReporters = initBuiltInReporters;
        this.ipRankItems = ipRankItems;
        this.lookupExtra = new LookupExtra(dnsId, dnsKey, token);
        this.timeoutMills = timeoutMills;
        this.protectedDomains = protectedDomains;
        this.preLookupDomains = preLookupDomains;
        this.enablePersistentCache = enablePersistentCache;
        this.persistentCacheDomains = persistentCacheDomains;
        this.channel = channel;
        this.enableReport = enableReport;
        this.blockFirst = blockFirst;
        this.customNetStack = customNetStack;
        this.executorSupplier = executorSupplier;
        this.lookedUpListener = lookedUpListener;
        this.logNodes = logNodes;
        this.reporters = reporters;
        this.useExpiredIpEnable = useExpiredIpEnable;
        this.cachedIpEnable = cachedIpEnable;
        this.routeIp = routeIp;
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
        return "DnsConfig{"
                + "logLevel=" + logLevel
                + ", appId='" + appId + '\''
                + ", userId='" + userId + '\''
                + ", lookupExtra=" + lookupExtra
                + ", timeoutMills=" + timeoutMills
                + ", protectedDomains=" + CommonUtils.toString(protectedDomains)
                + ", preLookupDomains=" + CommonUtils.toString(preLookupDomains)
                + ", enablePersistentCache=" + enablePersistentCache
                + ", persistentCacheDomains=" + CommonUtils.toString(persistentCacheDomains)
                + ", IpRankItems=" + CommonUtils.toString(ipRankItems)
                + ", channel='" + channel + '\''
                + ", enableReport='" + enableReport + '\''
                + ", blockFirst=" + blockFirst
                + ", customNetStack=" + customNetStack
                + ", executorSupplier=" + executorSupplier
                + ", lookedUpListener=" + lookedUpListener
                + ", logNodes=" + CommonUtils.toString(logNodes)
                + ", reporters=" + CommonUtils.toString(reporters)
                + ", useExpiredIpEnable=" + useExpiredIpEnable
                + ", cachedIpEnable=" + cachedIpEnable
                + ", enableDomainServer=" + enableDomainServer
                + ", routeIp=" + routeIp
                + '}';
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
        private static final int DEFAULT_MAX_NUM_OF_IP_RANK_ITEMS = 10;

        private int mLogLevel = Log.WARN;

        private String mAppId = "";
        private String mUserId = "";

        @Deprecated
        private boolean mInitBuiltInReporters = false;

        @Deprecated
        private String mDnsIp = "";
        private String mDnsId = "";
        private String mDnsKey = "";
        private String mToken = "";

        private int mTimeoutMills = 2000;

        private int mMaxNumOfPreLookupDomains = DEFAULT_MAX_NUM_OF_PRE_LOOKUP_DOMAINS;
        private int mMaxNumOfIpRankItems = DEFAULT_MAX_NUM_OF_IP_RANK_ITEMS;

        // mPreLookupDomains包含于mProtectedDomains

        private Set<WildcardDomain> mProtectedDomains = null;
        private Set<String> mPreLookupDomains = null;
        private Set<String> mPersistentCacheDomains = null;
        private Set<IpRankItem> mIpRankItems = null;
        private boolean mEnablePersistentCache = true;

        private String mChannel = Const.DES_HTTP_CHANNEL;
        private boolean mEnableReport = false;
        private boolean mBlockFirst = false;
        private int mCustomNetStack = 0;

        private DnsExecutors.ExecutorSupplier mExecutorSupplier = null;

        private ILookedUpListener mLookedUpListener = null;
        private List<ILogNode> mLogNodes = null;

        private List<IReporter> mReporters = null;
        private boolean mUseExpiredIpEnable = false;
        private boolean mCachedIpEnable = false;
        private String mRouteIp = "";

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
         * @param appId AppId, 从<a href="https://console.cloud.tencent.com/HttpDNS">腾讯云官网</a>申请获得
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
         * 启停缓存自动刷新功能, 默认开启
         *
         * @param enablePersistentCache, 启停缓存自动刷新功能
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder enablePersistentCache(boolean enablePersistentCache) {
            mEnablePersistentCache = enablePersistentCache;
            return this;
        }

        /**
         * 自动初始化内置上报通道
         * 不设置时, 默认为自动初始化内置上报通道
         *
         * @return 当前Builder实例, 方便链式调用
         */
        @Deprecated
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
        @Deprecated
        public Builder notInitBuiltInReporters() {
            mInitBuiltInReporters = false;
            return this;
        }

        /**
         * 设置DnsIp 【V4.5.0版本起废弃】
         *
         * @param dnsIp HTTPDNS IP 地址
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException dnsIp为空时抛出
         */
        @Deprecated
        public Builder dnsIp(String dnsIp) {
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
         * @param dnsKey dnsKey, 即HTTPDNS服务的授权Id对应的加密密钥, 从<a href="https://console.cloud.tencent.com/HttpDNS">腾讯云官网</a>
         *               申请获得
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
            if (mChannel == Const.HTTPS_CHANNEL && TextUtils.isEmpty(token)) {
                throw new IllegalArgumentException("token".concat(Const.EMPTY_TIPS));
            }
            mToken = token;
            return this;
        }


        /**
         * 设置域名解析请求超时时间
         * 不设置时, 默认为2000ms
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
//            调整：保活域名独立于预解析域名
//            if (null != mPersistentCacheDomains) {
//                Iterator<String> domainIterator = mPersistentCacheDomains.iterator();
//                while (domainIterator.hasNext()) {
//                    String domain = domainIterator.next();
//                    if (!mPreLookupDomains.contains(domain)) {
//                        domainIterator.remove();
//                    }
//                }
//            }

            return this;
        }

        /**
         * 设置保活域名, 保活域名在解析缓存过期前会通过后台线程进行静默解析
         * 不设置时, 默认不会进行提前解析
         *
         * @param domains 保活域名
         * @return 当前Builder实例, 方便链式调用
         * @throws IllegalArgumentException domains为空时抛出
         */
        public synchronized Builder persistentCacheDomains(String... domains) {
            if (CommonUtils.isEmpty(domains)) {
                throw new IllegalArgumentException("domains".concat(Const.EMPTY_TIPS));
            }

            if (null == mPersistentCacheDomains) {
                mPersistentCacheDomains = CollectionCompat.createSet(domains.length);
            }

            for (String domain : domains) {
                if (TextUtils.isEmpty(domain) || TextUtils.isEmpty(domain = domain.trim())) {
                    throw new IllegalArgumentException("domain".concat(Const.EMPTY_TIPS));
                }

                mPersistentCacheDomains.add(domain);
            }

            return this;
        }

        /**
         * 设置IP优选服务，启动IP优选服务的域名会进行连接竞速更新缓存，下次解析时返回最优的IP
         *
         * @param ipRankItems 域名优选配置列表{hostname, port}
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder ipRankItems(List<IpRankItem> ipRankItems) {
            if (ipRankItems.size() > mMaxNumOfIpRankItems) {
                mIpRankItems = new HashSet<>(ipRankItems.subList(0, mMaxNumOfIpRankItems));
            } else {
                mIpRankItems = new HashSet<>(ipRankItems);
            }
            return this;
        }

        public Builder channel(String channel) {
            if (channel.equals(Const.HTTPS_CHANNEL) && BuildConfig.FLAVOR.equals("intl")) {
                throw new IllegalArgumentException("httpdns-sdk-intl version still doesn't support " + Const.HTTPS_CHANNEL);
            }
            mChannel = channel;
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
            if (BuildConfig.FLAVOR.equals("intl")) {
                throw new IllegalArgumentException("httpdns-sdk-intl version still doesn't support " + Const.HTTPS_CHANNEL);
            }
            mChannel = Const.HTTPS_CHANNEL;
            return this;
        }

        /**
         * 解析日志上报开关，【V4.4.0废弃】
         *
         * @param enableReport
         * @return
         */
        @Deprecated
        public Builder enableReport(boolean enableReport) {
//            mEnableReport = enableReport;
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

        public Builder setCustomNetStack(int customNetStack) {
            mCustomNetStack = customNetStack;
            return this;
        }

        /**
         * 允许使用过期缓存
         *
         * @param useExpiredIpEnable 默认false，解析时先取未过期的缓存结果，不满足则等待解析请求完成后返回解析结果
         *                           设置为true时，会直接返回缓存的解析结果，没有缓存则返回0;
         *                           0，用户可使用localdns（InetAddress）进行兜底。且在无缓存结果或缓存已过期时，会异步发起解析请求更新缓存。
         *                           因异步API（getAddrByNameAsync，getAddrsByNameAsync）逻辑在回调中始终返回未过期的解析结果，设置为true时，异步API
         *                           不可使用。建议使用同步API （getAddrByName，getAddrsByName）
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder setUseExpiredIpEnable(boolean useExpiredIpEnable) {
            mUseExpiredIpEnable = useExpiredIpEnable;

            return this;
        }

        /**
         * 启用本地缓存（Room）实现持久化缓存
         *
         * @param cachedIpEnable 默认false
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder setCachedIpEnable(boolean cachedIpEnable) {
            mCachedIpEnable = cachedIpEnable;
            return this;
        }

        /**
         * 设置DNS 请求的 ECS（EDNS-Client-Subnet）值
         *
         * @param routeIp IPV4/IPv6 地址值
         * @return 当前Builder实例, 方便链式调用
         */
        public Builder routeIp(String routeIp) {
            mRouteIp = routeIp;
            return this;
        }

        /**
         * 构建DnsConfig实例
         *
         * @return DnsConfig实例
         */
        public DnsConfig build() {
            return new DnsConfig(mLogLevel,
                    mAppId, mUserId, mInitBuiltInReporters, mDnsId, mDnsKey, mToken,
                    mTimeoutMills,
                    mProtectedDomains, mPreLookupDomains, mEnablePersistentCache, mPersistentCacheDomains,
                    mIpRankItems, mChannel, mEnableReport, mBlockFirst,
                    mCustomNetStack, mExecutorSupplier,
                    mLookedUpListener, mLogNodes,
                    mReporters, mUseExpiredIpEnable, mCachedIpEnable, mRouteIp);
        }
    }
}
