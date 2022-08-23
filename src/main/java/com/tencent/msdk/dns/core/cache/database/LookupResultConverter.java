package com.tencent.msdk.dns.core.cache.database;

import androidx.room.TypeConverter;

import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.core.IpSet;
import com.tencent.msdk.dns.core.LookupResult;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

import org.json.JSONException;
import org.json.JSONObject;

public class LookupResultConverter {
    @TypeConverter
    public String fromLookupResult(LookupResult lookupResult) {
        return lookupResult.toString();
    }

    @TypeConverter
    public LookupResult toLookupResult(String lookupResult) {
        try {
            JSONObject json = new JSONObject(lookupResult);
            if (json.has("ipSet")) {
                return new LookupResult(IpSet.EMPTY, new StatisticsMerge(DnsService.getAppContext()));
//                return new LookupResult((IpSet) json.get("ipSet"), (StatisticsMerge)json.get("stat"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new LookupResult(IpSet.EMPTY, new StatisticsMerge(DnsService.getAppContext()));
    }
}
