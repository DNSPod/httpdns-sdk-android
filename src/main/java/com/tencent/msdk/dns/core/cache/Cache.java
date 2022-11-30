package com.tencent.msdk.dns.core.cache;

import android.text.TextUtils;

import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.ICache;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.cache.database.LookupCache;
import com.tencent.msdk.dns.core.cache.database.LookupCacheDao;
import com.tencent.msdk.dns.core.cache.database.LookupCacheDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Cache implements ICache {

    private static final Map<String, LookupResult> mHostnameIpsMap = new ConcurrentHashMap<>();

    private static final LookupCacheDao lookupCacheDao = LookupCacheDatabase.getInstance(DnsService.getAppContext()).lookupCacheDao();

    private static boolean getCachedIpEnable() {
        return DnsService.getDnsConfig().cachedIpEnable;
    }

    private static boolean isIntlServerDomain(String hostname) {
        return BuildConfig.FLAVOR.equals("intl") && hostname.equals(BuildConfig.INIT_SERVERS_DOMAINS[0]);
    }

    public static void readFromDb() {
        if (getCachedIpEnable()) {
            List<LookupCache> allCache = lookupCacheDao.getAll();
            ArrayList<LookupCache> expired = new ArrayList<LookupCache>();
            for (LookupCache lookupCache : allCache) {
                mHostnameIpsMap.put(lookupCache.hostname, lookupCache.lookupResult);

                if (lookupCache.isExpired()) {
                    expired.add(lookupCache);
                }
            }
            // 内存读取后，清空本地已过期的缓存
            lookupCacheDao.deleteLookupCaches(expired);
        } else {
            // 服务域名缓存读取
            readServerFromDb();
        }
    }

    private static void readServerFromDb() {
        // 国际站域名服务按州分布，持久化缓存
        if (BuildConfig.FLAVOR.equals("intl")) {
            LookupCache serverCache = lookupCacheDao.get(BuildConfig.INIT_SERVERS_DOMAINS[0]);
            mHostnameIpsMap.put(serverCache.hostname, serverCache.lookupResult);

            if (serverCache.isExpired()) {
                lookupCacheDao.delete(serverCache.hostname);
            }
        }
    }

    @Override
    public LookupResult get(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        return mHostnameIpsMap.get(hostname);
    }

    @Override
    public void add(String hostname, LookupResult lookupResult) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }

        DnsLog.d("Cache %s for %s", lookupResult, hostname);
        mHostnameIpsMap.put(hostname, lookupResult);
        if (getCachedIpEnable() || isIntlServerDomain(hostname)) {
            lookupCacheDao.insertLookupCache(new LookupCache(hostname, lookupResult));
        }
    }

    @Override
    public void delete(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        mHostnameIpsMap.remove(hostname);

        if (getCachedIpEnable()) {
            lookupCacheDao.delete(hostname);
        }

    }

    @Override
    public void clear() {
        mHostnameIpsMap.clear();

        if (getCachedIpEnable()) {
            lookupCacheDao.clear();
        }
    }
}
