package com.tencent.msdk.dns.core.rest.share;

import com.tencent.msdk.dns.core.LookupResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

@Deprecated
public final class AsyncLookupResultQueue {

    private static final List<LookupResult> DNS_RESULTS = new Vector<>();

    public static void enqueue(LookupResult lookupResult) {
        DNS_RESULTS.add(lookupResult);
    }

    public static List<LookupResult> offerAll() {
        List<LookupResult> lookupResults;
        synchronized (DNS_RESULTS) {
            if (DNS_RESULTS.isEmpty()) {
                lookupResults = Collections.emptyList();
            } else {
                lookupResults = new ArrayList<>(DNS_RESULTS);
                DNS_RESULTS.clear();
            }
        }
        return lookupResults;
    }
}
