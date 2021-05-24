package com.tencent.msdk.dns.core;

public interface ILookupListener {

    void onLookedUp(LookupParameters lookupParameters, LookupResult<IStatisticsMerge> lookupResult);
}
