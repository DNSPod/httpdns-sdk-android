package com.tencent.msdk.dns.core.cache.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.tencent.msdk.dns.core.LookupResult;

@Entity
public class LookupCache {
    @PrimaryKey
    @NonNull
    public String hostname;

    @ColumnInfo
    public LookupResult lookupResult;

    public LookupCache(String mHostname, LookupResult mLookupResult) {
        this.hostname = mHostname;
        this.lookupResult = mLookupResult;
    }

    public LookupCache() {
    }

    @NonNull
    public String getHostname() {
        return hostname;
    }

    public LookupResult getLookupResult() {
        return lookupResult;
    }
}

