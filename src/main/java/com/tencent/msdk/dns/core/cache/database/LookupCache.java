package com.tencent.msdk.dns.core.cache.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LookupCache {
    @PrimaryKey
    @NonNull
    public String hostname;

    @ColumnInfo
//    @TypeConverters(LookupResultConverter.class)
    public byte[] lookupResult;

    public LookupCache(String mHostname, byte[] mLookupResult) {
        this.hostname = mHostname;
        this.lookupResult = mLookupResult;
    }

    public LookupCache() {
    }
}

