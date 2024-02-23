package com.tencent.msdk.dns.core.cache;

import android.text.TextUtils;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.ICache;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.cache.database.CacheDbHelper;
import com.tencent.msdk.dns.core.cache.database.LookupCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Cache implements ICache {

    private final Map<String, LookupResult> mHostnameIpsMap = new ConcurrentHashMap<>();

    private final CacheDbHelper cacheDbHelper = new CacheDbHelper(DnsService.getContext());

    private boolean getCachedIpEnable() {
        return DnsService.getDnsConfig().cachedIpEnable;
    }

    private Cache() {
    }

    private static final class CacheHolder {
        static final Cache instance = new Cache();
    }

    public static Cache getInstance() {
        return CacheHolder.instance;
    }

    public void readFromDb() {
        if (getCachedIpEnable()) {
            List<LookupCache> allCache = cacheDbHelper.getAll();
            ArrayList<LookupCache> expired = new ArrayList<>();
            for (LookupCache lookupCache : allCache) {
                mHostnameIpsMap.put(lookupCache.hostname, lookupCache.lookupResult);

                if (lookupCache.isExpired()) {
                    expired.add(lookupCache);
                }
            }
            // 内存读取后，清空本地已过期的缓存
            cacheDbHelper.deleteLookupCaches(expired);
        }
    }

    public void clearCache(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            CacheHolder.instance.clear();
        } else {
            String[] hostnameArr = hostname.split(",");
            if (hostnameArr.length > 1) {
                for (String tempHostname : hostnameArr) {
                    CacheHolder.instance.delete(tempHostname);
                }
            } else {
                CacheHolder.instance.delete(hostname);
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
        if (getCachedIpEnable()) {
            cacheDbHelper.insertLookupCache(new LookupCache(hostname, lookupResult));
        }
    }

    @Override
    public void delete(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        mHostnameIpsMap.remove(hostname);

        if (getCachedIpEnable()) {
            cacheDbHelper.delete(hostname);
        }

    }

    @Override
    public void clear() {
        mHostnameIpsMap.clear();

        if (getCachedIpEnable()) {
            cacheDbHelper.clear();
        }
    }
}
