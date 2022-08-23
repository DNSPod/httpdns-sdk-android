package com.tencent.msdk.dns.core.cache.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;

@Database(entities = {LookupCache.class}, version = 3, exportSchema = false)
public abstract class LookupCacheDatabase extends RoomDatabase {
    private static final String DB_NAME = "lookup_result_db";

    private static LookupCacheDatabase instance;

    public abstract LookupCacheDao lookupCacheDao();

    public static synchronized LookupCacheDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), LookupCacheDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

}
