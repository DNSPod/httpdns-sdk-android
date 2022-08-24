package com.tencent.msdk.dns.core.cache;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.ICache;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.cache.database.LookupCache;
import com.tencent.msdk.dns.core.cache.database.LookupCacheDao;
import com.tencent.msdk.dns.core.cache.database.LookupCacheDatabase;
import com.tencent.msdk.dns.core.rest.share.AbsRestDns;

public class Database implements ICache {
    LookupCacheDao lookupCacheDao = LookupCacheDatabase.getInstance(DnsService.getAppContext()).lookupCacheDao();

    @Override
    public LookupResult get(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        String lookupResultStr = lookupCacheDao.get(hostname);
        try {
            if (lookupResultStr != null) {
                Gson gson = new Gson();
                java.lang.reflect.Type type = new TypeToken<LookupResult<AbsRestDns.Statistics>>() {
                }.getType();
                LookupResult lookupResult = gson.fromJson(lookupResultStr, type);
                return lookupResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void add(String hostname, LookupResult lookupResult) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }
        if (null == lookupResult) {
            throw new IllegalArgumentException("lookupResult".concat(Const.NULL_POINTER_TIPS));
        }

        DnsLog.d("hello-----add");

//        lookupCacheDao.clear();

        if (lookupCacheDao.get(hostname) != null) {
            lookupCacheDao.updateLookupCache(new LookupCache(hostname, lookupResult.toJsonString()));
        }
        Gson gson = new Gson();
        lookupCacheDao.insertLookupCache(new LookupCache(hostname, gson.toJson(lookupResult)));
    }

    @Override
    public void delete(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        lookupCacheDao.delete(hostname);
//        lookupCacheDao.delete(new LookupCache(hostname, lookupCacheDao.get(hostname)));
    }

    @Override
    public void clear() {
        lookupCacheDao.clear();
    }
}
