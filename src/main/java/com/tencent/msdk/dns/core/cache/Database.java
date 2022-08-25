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
import com.tencent.msdk.dns.core.rest.share.AbsRestDns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Database implements ICache {
    LookupCacheDao lookupCacheDao = LookupCacheDatabase.getInstance(DnsService.getAppContext()).lookupCacheDao();

    @Override
    public LookupResult get(String hostname) {
        if (TextUtils.isEmpty(hostname)) {
            throw new IllegalArgumentException("hostname".concat(Const.EMPTY_TIPS));
        }

        long start = System.currentTimeMillis();
        byte[] lookupResultByteArr = lookupCacheDao.get(hostname);
        DnsLog.d("database waste time1: " + String.valueOf(System.currentTimeMillis() - start));
        if (lookupResultByteArr != null) {
            ByteArrayInputStream bais = null;
            ObjectInputStream ois = null;
            try {
                bais = new ByteArrayInputStream(lookupResultByteArr);
                ois = new ObjectInputStream(bais);
                LookupResult lookupResult = (LookupResult<AbsRestDns.Statistics>) ois.readObject();
                DnsLog.d("database waste time2: " + String.valueOf(System.currentTimeMillis() - start));
                return lookupResult;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bais != null) {
                        bais.close();
                    }
                    if (ois != null) {
                        ois.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
//        if (lookupCacheDao.get(hostname) != null) {
//            lookupCacheDao.updateLookupCache(new LookupCache(hostname, lookupResult.toJsonString()));
//        }
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(lookupResult);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lookupCacheDao.insertLookupCache(new LookupCache(hostname, baos.toByteArray()));
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
