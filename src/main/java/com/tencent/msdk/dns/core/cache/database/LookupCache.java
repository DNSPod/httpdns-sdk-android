package com.tencent.msdk.dns.core.cache.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.core.IpSet;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

@Entity
public class LookupCache {
    @PrimaryKey
    @NonNull
    public String hostname;

    @ColumnInfo
//    @TypeConverters(LookupResultConverter.class)
    public String lookupResult;

    public LookupCache(String mHostname, String mLookupResult) {
        this.hostname = mHostname;
        this.lookupResult = mLookupResult;
    }

    public LookupCache() {
    }
}

//@Entity
//public class LookupCache {
//    @PrimaryKey
//    @NonNull
//    public String hostname;
//
//    @Entity
//    public class LookupResult {
//        @Entity
//        public class IpSet{
//            @ColumnInfo
//            private String[] v4Ips;
//            private String[] v6Ips;
//            private String[] ips;
//        }
//
//        @Entity
//        public class Stati{
//            @ColumnInfo
//            private String[] v4Ips;
//            private String[] v6Ips;
//            private String[] ips;
//        }
//    }
//}

