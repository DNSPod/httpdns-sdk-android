package com.tencent.msdk.dns.core.cache;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.ICache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Cache implements ICache {

    private final Map<String, LookupResult> mHostnameIpsMap = new ConcurrentHashMap<>();

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
    }

    @Override
    public void delete(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        mHostnameIpsMap.remove(hostname);
    }

    @Override
    public void clear() {
        mHostnameIpsMap.clear();
    }
}
