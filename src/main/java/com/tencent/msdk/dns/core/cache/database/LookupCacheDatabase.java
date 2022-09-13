package com.tencent.msdk.dns.core.cache.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.tencent.msdk.dns.base.log.DnsLog;

@Database(entities = {LookupCache.class}, version = 2, exportSchema = false)
@TypeConverters(LookupResultConverter.class)
public abstract class LookupCacheDatabase extends RoomDatabase {
    private static final String DB_NAME = "lookup_result_db";

    private static LookupCacheDatabase instance;

    public abstract LookupCacheDao lookupCacheDao();

    public static synchronized LookupCacheDatabase getInstance(Context context) {
        if (instance == null) {
            creat(context);
        }
        return instance;
    }

    public static void creat(Context context) {
        instance = Room.databaseBuilder(context.getApplicationContext(), LookupCacheDatabase.class, DB_NAME)
                .fallbackToDestructiveMigration()
                .build();
    }
}
