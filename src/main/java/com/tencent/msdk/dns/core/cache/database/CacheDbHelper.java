package com.tencent.msdk.dns.core.cache.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CacheDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "LookupResult.db";

    private static final Object mLock = new Object();

    private SQLiteDatabase mDb;

    static class DB {
        static final String TABLE_NAME = "lookupDB";

        static final String HOST = "host";

        static final String RESULT = "result";

        static final String SQL_CREATE_ENTRIES = "CREATE TABLE " + DB.TABLE_NAME + " ("
                + HOST + " TEXT PRIMARY KEY,"
                + RESULT + " TEXT)";

        static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public CacheDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private SQLiteDatabase getDb() {
        if (mDb == null) {
            try {
                mDb = getWritableDatabase();
            } catch (Exception e) {
                DnsLog.e("get db error " + e);
            }
        }
        return mDb;
    }

    public List<LookupCache> getAll() {
        synchronized (mLock) {
            ArrayList<LookupCache> lists = new ArrayList<>();
            SQLiteDatabase db;
            Cursor cursor = null;

            try {
                db = getDb();
                cursor = db.query(DB.TABLE_NAME, null, null, null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    do {
                        lists.add(new LookupCache(
                                cursor.getString(cursor.getColumnIndex(DB.HOST)),
                                LookupResultConverter.toLookupResult(cursor.getBlob(cursor.getColumnIndex(DB.RESULT)))
                        ));
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                DnsLog.e("read from db fail " + e);
            } finally {
                try {
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e) {
                    DnsLog.e("cursor close error " + e);
                }
            }
            return lists;
        }
    }

    public void insert(LookupCache lookupCache) {
        synchronized (mLock) {
            SQLiteDatabase db = null;
            try {
                db = getDb();
                db.beginTransaction();
                ContentValues cv = new ContentValues();
                cv.put(DB.HOST, lookupCache.hostname);
                cv.put(DB.RESULT, LookupResultConverter.fromLookupResult(lookupCache.lookupResult));
                db.insertWithOnConflict(DB.TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                DnsLog.e("insert lookupCache fail " + e);
            } finally {
                if (db != null) {
                    try {
                        db.endTransaction();
                    } catch (Exception e) {
                        DnsLog.e("db end transaction error  " + e);
                    }
                }
            }
        }
    }

    public void delete(String hostname) {
        delete(new String[]{hostname});
    }

    public void delete(String[] hosts) {
        if (hosts.length > 0) {
            synchronized (mLock) {
                SQLiteDatabase db = null;
                try {
                    db = getDb();
                    db.beginTransaction();
                    db.delete(DB.TABLE_NAME, DB.HOST + " IN (" + TextUtils.join(",", Collections.nCopies(hosts.length,
                                    "?")) + ")",
                            hosts);
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    DnsLog.e("delete by hostname fail" + e);
                } finally {
                    if (db != null) {
                        try {
                            db.endTransaction();
                        } catch (Exception e) {
                            DnsLog.e("db end transaction error " + e);
                        }
                    }
                }
            }
        }
    }

    public void clear() {
        synchronized (mLock) {
            SQLiteDatabase db = null;
            try {
                db = getDb();
                db.beginTransaction();
                db.delete(DB.TABLE_NAME, null, null);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                DnsLog.e("clear cache fail" + e);
            } finally {
                if (db != null) {
                    try {
                        db.endTransaction();
                    } catch (Exception e) {
                        DnsLog.e("db end transaction error " + e);
                    }
                }
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(DB.SQL_CREATE_ENTRIES);
        } catch (Exception e) {
            DnsLog.e("create db fail " + e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            try {
                db.execSQL(DB.SQL_DELETE_ENTRIES);
                onCreate(db);
            } catch (Exception e) {
                DnsLog.e("upgrade db fail " + e);
            }
        }
    }
}
