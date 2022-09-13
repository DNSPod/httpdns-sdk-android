package com.tencent.msdk.dns.core.cache.database;

import androidx.room.ColumnInfo;

import com.tencent.msdk.dns.core.LookupResult;

public class LookupCacheResult {
    @ColumnInfo(name = "lookupResult")
    public LookupResult lookupResult;
}
