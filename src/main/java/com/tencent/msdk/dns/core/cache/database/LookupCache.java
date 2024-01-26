package com.tencent.msdk.dns.core.cache.database;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.rest.share.AbsRestDns;

@Entity
public class LookupCache {
    @PrimaryKey
    @NonNull
    public String hostname;

    @ColumnInfo
    public LookupResult lookupResult;

    public LookupCache(@NonNull String mHostname, LookupResult mLookupResult) {
        this.hostname = mHostname;
        this.lookupResult = mLookupResult;
    }

    public LookupCache() {
    }

    public boolean isExpired() {
        AbsRestDns.Statistics stat = (AbsRestDns.Statistics) lookupResult.stat;
        if (stat != null) {
            return SystemClock.elapsedRealtime() > stat.expiredTime;
        }
        return true;
    }
}

