package com.tencent.msdk.dns.core;

/**
 * 需保证线程安全
 */
public interface ICache {

    /* @Nullable */
    LookupResult get(String hostname);

    void add(String hostname, LookupResult lookupResult);

    void delete(String hostname);

    void clear();
}
