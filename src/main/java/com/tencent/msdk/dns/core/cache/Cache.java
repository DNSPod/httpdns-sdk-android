package com.tencent.msdk.dns.core.cache;

import android.text.TextUtils;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.ICache;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.cache.database.LookupCache;
import com.tencent.msdk.dns.core.cache.database.LookupCacheDao;
import com.tencent.msdk.dns.core.cache.database.LookupCacheDatabase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Cache implements ICache {

    private static final Map<String, LookupResult> mHostnameIpsMap = new ConcurrentHashMap<>();

    private static final LookupCacheDao lookupCacheDao = LookupCacheDatabase.getInstance(DnsService.getAppContext()).lookupCacheDao();

    private boolean getCachedIpEnable() {
        return DnsService.getDnsConfig().cachedIpEnable;
    }

    public static void readFromDb() {
        List<LookupCache> allCache = lookupCacheDao.getAll();
        for (LookupCache lookupCache : allCache) {
            mHostnameIpsMap.put(lookupCache.getHostname(), lookupCache.getLookupResult());
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
        if (getCachedIpEnable()) {
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
