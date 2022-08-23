package com.tencent.msdk.dns.core.cache.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.cache.database.LookupCache;

import java.util.List;

@Dao
public interface LookupCacheDao {
    @Query("Select * from lookupcache")
    List<LookupCache> getAll();

    @Query("Select lookupResult from lookupcache where hostname = :hostname")
    String get(String hostname);

//    @Query("Select * from lookupcache where domain in(:domain)")
//    LookupCache getLookupResultByHostname(String domain);

    @Insert
    void insertLookupCache(LookupCache lookupCache);

    @Update
    void updateLookupCache(LookupCache lookupCache);

    @Delete
    void delete(LookupCache lookupCache);

    @Query("delete from lookupcache where hostname = :hostname")
    void delete(String hostname);

    @Query("delete from lookupcache")
    void clear();
}
